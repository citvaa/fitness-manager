package com.example.demo.service.impl;

import com.example.demo.dto.PaymentDTO;
import com.example.demo.mapper.PaymentMapper;
import com.example.demo.model.Payment;
import com.example.demo.model.Session;
import com.example.demo.model.user.Client;
import com.example.demo.model.user.ClientSessionTracking;
import com.example.demo.repository.PaymentRepository;
import com.example.demo.repository.SessionRepository;
import com.example.demo.repository.user.ClientRepository;
import com.example.demo.repository.user.ClientSessionTrackingRepository;
import com.example.demo.service.params.request.user.client.CreatePaymentRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link PaymentServiceImpl} - the Faza 6 payment-history read endpoints
 * ({@code getAll(clientId)}'s optional filter, {@code getMyPayments()}'s JWT resolution) plus
 * the pre-existing {@code create()} tracking-update logic, for completeness. See AGENTS.md
 * "Upgrade: Faza 6 decisions (continued)".
 */
@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    private PaymentMapper paymentMapper;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private ClientRepository clientRepository;
    @Mock
    private SessionRepository sessionRepository;
    @Mock
    private ClientSessionTrackingRepository clientSessionTrackingRepository;

    private PaymentServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new PaymentServiceImpl(paymentMapper, paymentRepository, clientRepository,
                sessionRepository, clientSessionTrackingRepository);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void create_throwsWhenPaidAppointmentsIsZeroOrNegative() {
        CreatePaymentRequest request = new CreatePaymentRequest(1, 1, 0, LocalDate.now());

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("greater than zero");

        verifyNoInteractions(clientRepository, sessionRepository, paymentRepository);
    }

    @Test
    void create_topsUpExistingTrackingRow() {
        CreatePaymentRequest request = new CreatePaymentRequest(1, 2, 10, LocalDate.now());
        Client client = Client.builder().id(1).build();
        Session session = new Session(2, com.example.demo.enums.SessionType.INDIVIDUAL, 1);
        ClientSessionTracking tracking = ClientSessionTracking.builder()
                .client(client).session(session).remainingAppointments(5).reservedAppointments(0).build();

        when(clientRepository.findById(1)).thenReturn(Optional.of(client));
        when(sessionRepository.findById(2)).thenReturn(Optional.of(session));
        when(clientSessionTrackingRepository.findByClientAndSession(client, session)).thenReturn(Optional.of(tracking));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(paymentMapper.toDto(any(Payment.class))).thenReturn(new PaymentDTO());

        service.create(request);

        assertThat(tracking.getRemainingAppointments()).isEqualTo(15);
        verify(clientSessionTrackingRepository).save(tracking);

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(captor.capture());
        assertThat(captor.getValue().getPaidAppointments()).isEqualTo(10);
    }

    @Test
    void create_createsFreshTrackingRowWhenNoneExistsYet() {
        CreatePaymentRequest request = new CreatePaymentRequest(1, 2, 8, LocalDate.now());
        Client client = Client.builder().id(1).build();
        Session session = new Session(2, com.example.demo.enums.SessionType.INDIVIDUAL, 1);

        when(clientRepository.findById(1)).thenReturn(Optional.of(client));
        when(sessionRepository.findById(2)).thenReturn(Optional.of(session));
        when(clientSessionTrackingRepository.findByClientAndSession(client, session)).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(paymentMapper.toDto(any(Payment.class))).thenReturn(new PaymentDTO());

        service.create(request);

        ArgumentCaptor<ClientSessionTracking> captor = ArgumentCaptor.forClass(ClientSessionTracking.class);
        verify(clientSessionTrackingRepository).save(captor.capture());
        assertThat(captor.getValue().getRemainingAppointments()).isEqualTo(8);
    }

    @Test
    void create_throwsWhenClientNotFound() {
        CreatePaymentRequest request = new CreatePaymentRequest(1, 2, 8, LocalDate.now());
        when(clientRepository.findById(1)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Client not found");
    }

    @Test
    void getAll_noFilter_returnsEveryPayment() {
        when(paymentRepository.findAllByOrderByPaymentDateDesc()).thenReturn(List.of(new Payment(), new Payment()));
        when(paymentMapper.toDto(anyList())).thenReturn(List.of(new PaymentDTO(), new PaymentDTO()));

        List<PaymentDTO> result = service.getAll(null);

        assertThat(result).hasSize(2);
        verify(paymentRepository, never()).findByClientIdOrderByPaymentDateDesc(any());
    }

    @Test
    void getAll_withClientIdFilter_scopesToThatClientOnly() {
        when(paymentRepository.findByClientIdOrderByPaymentDateDesc(3)).thenReturn(List.of(new Payment()));
        when(paymentMapper.toDto(anyList())).thenReturn(List.of(new PaymentDTO()));

        List<PaymentDTO> result = service.getAll(3);

        assertThat(result).hasSize(1);
        verify(paymentRepository, never()).findAllByOrderByPaymentDateDesc();
    }

    @Test
    void getMyPayments_resolvesClientFromJwt() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("email", "client@gym.com")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(900))
                .build();
        Authentication authentication = new UsernamePasswordAuthenticationToken(jwt, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        Client client = Client.builder().id(42).build();
        when(clientRepository.findByUserEmail("client@gym.com")).thenReturn(Optional.of(client));
        when(paymentRepository.findByClientIdOrderByPaymentDateDesc(42)).thenReturn(List.of(new Payment()));
        when(paymentMapper.toDto(anyList())).thenReturn(List.of(new PaymentDTO()));

        List<PaymentDTO> result = service.getMyPayments();

        assertThat(result).hasSize(1);
    }

    @Test
    void getMyPayments_throwsWhenUnauthenticated() {
        assertThatThrownBy(() -> service.getMyPayments()).isInstanceOf(AccessDeniedException.class);
    }
}
