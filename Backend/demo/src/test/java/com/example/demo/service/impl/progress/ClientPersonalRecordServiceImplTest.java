package com.example.demo.service.impl.progress;

import com.example.demo.dto.progress.ClientPersonalRecordDTO;
import com.example.demo.enums.RecordUnit;
import com.example.demo.mapper.progress.ClientPersonalRecordMapper;
import com.example.demo.model.progress.ClientPersonalRecord;
import com.example.demo.model.user.Client;
import com.example.demo.repository.progress.ClientPersonalRecordRepository;
import com.example.demo.repository.user.ClientRepository;
import com.example.demo.security.TrainerClientAccessGuard;
import com.example.demo.service.params.request.progress.CreatePersonalRecordRequest;
import jakarta.persistence.EntityNotFoundException;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link ClientPersonalRecordServiceImpl} - mirrors
 * {@link ClientProgressEntryServiceImplTest}'s coverage of the trainer-ownership guard and the
 * JWT-resolved getMine() path.
 */
@ExtendWith(MockitoExtension.class)
class ClientPersonalRecordServiceImplTest {

    @Mock
    private ClientPersonalRecordRepository clientPersonalRecordRepository;
    @Mock
    private ClientRepository clientRepository;
    @Mock
    private ClientPersonalRecordMapper clientPersonalRecordMapper;
    @Mock
    private TrainerClientAccessGuard trainerClientAccessGuard;

    private ClientPersonalRecordServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ClientPersonalRecordServiceImpl(clientPersonalRecordRepository, clientRepository,
                clientPersonalRecordMapper, trainerClientAccessGuard);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void create_delegatesOwnershipCheckToGuardBeforeSaving() {
        CreatePersonalRecordRequest request = new CreatePersonalRecordRequest();
        request.setClientId(1);
        request.setExerciseName("Bench press");
        request.setValue(new BigDecimal("100"));
        request.setUnit(RecordUnit.KG);
        request.setRecordDate(LocalDate.now());

        Client client = Client.builder().id(1).build();
        when(clientRepository.findById(1)).thenReturn(Optional.of(client));
        when(clientPersonalRecordRepository.save(any(ClientPersonalRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(clientPersonalRecordMapper.toDto(any(ClientPersonalRecord.class))).thenReturn(new ClientPersonalRecordDTO());

        service.create(request);

        verify(trainerClientAccessGuard).assertCanAccessClient(1);

        ArgumentCaptor<ClientPersonalRecord> captor = ArgumentCaptor.forClass(ClientPersonalRecord.class);
        verify(clientPersonalRecordRepository).save(captor.capture());
        assertThat(captor.getValue().getClient()).isSameAs(client);
        assertThat(captor.getValue().getExerciseName()).isEqualTo("Bench press");
        assertThat(captor.getValue().getValue()).isEqualTo(new BigDecimal("100"));
        assertThat(captor.getValue().getUnit()).isEqualTo(RecordUnit.KG);
    }

    @Test
    void create_propagatesAccessDeniedFromGuardAndNeverSaves() {
        CreatePersonalRecordRequest request = new CreatePersonalRecordRequest();
        request.setClientId(1);

        doThrow(new AccessDeniedException("denied")).when(trainerClientAccessGuard).assertCanAccessClient(1);

        assertThatThrownBy(() -> service.create(request)).isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(clientPersonalRecordRepository);
        verifyNoInteractions(clientRepository);
    }

    @Test
    void create_throwsWhenClientNotFound() {
        CreatePersonalRecordRequest request = new CreatePersonalRecordRequest();
        request.setClientId(1);
        when(clientRepository.findById(1)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Client not found");
    }

    @Test
    void getForClient_delegatesOwnershipCheckToGuard() {
        when(clientPersonalRecordRepository.findByClientIdOrderByRecordDateDesc(3)).thenReturn(List.of());
        when(clientPersonalRecordMapper.toDto(List.<ClientPersonalRecord>of())).thenReturn(List.of());

        service.getForClient(3);

        verify(trainerClientAccessGuard).assertCanAccessClient(3);
    }

    @Test
    void getForClient_propagatesAccessDenied() {
        doThrow(new AccessDeniedException("denied")).when(trainerClientAccessGuard).assertCanAccessClient(3);

        assertThatThrownBy(() -> service.getForClient(3)).isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(clientPersonalRecordRepository);
    }

    @Test
    void getMine_resolvesClientFromJwtAndNeverCallsTheGuard() {
        Client client = Client.builder().id(42).build();
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("email", "client@gym.com")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(900))
                .build();
        Authentication authentication = new UsernamePasswordAuthenticationToken(jwt, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        when(clientRepository.findByUserEmail("client@gym.com")).thenReturn(Optional.of(client));
        when(clientPersonalRecordRepository.findByClientIdOrderByRecordDateDesc(42)).thenReturn(List.of());
        when(clientPersonalRecordMapper.toDto(List.<ClientPersonalRecord>of())).thenReturn(List.of());

        service.getMine();

        verifyNoInteractions(trainerClientAccessGuard);
        verify(clientPersonalRecordRepository).findByClientIdOrderByRecordDateDesc(42);
    }

    @Test
    void getMine_throwsWhenUnauthenticated() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(() -> service.getMine()).isInstanceOf(AccessDeniedException.class);
    }
}
