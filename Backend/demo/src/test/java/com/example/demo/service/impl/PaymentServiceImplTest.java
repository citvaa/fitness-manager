package com.example.demo.service.impl;

import com.example.demo.dto.PaymentDTO;
import com.example.demo.mapper.PaymentMapper;
import com.example.demo.model.Payment;
import com.example.demo.model.Appointment;
import com.example.demo.model.Session;
import com.example.demo.model.gym.Gym;
import com.example.demo.enums.SessionType;
import com.example.demo.repository.AppointmentRepository;
import com.example.demo.repository.gym.GymRepository;
import com.example.demo.model.user.Client;
import com.example.demo.repository.PaymentRepository;
import com.example.demo.repository.SessionRepository;
import com.example.demo.repository.user.ClientRepository;
import com.example.demo.repository.user.ClientSessionTrackingRepository;
import com.example.demo.service.security.AuthenticatedUserService;
import com.example.demo.service.notification.NotificationService;
import com.example.demo.service.params.request.user.client.CreatePaymentRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {
    @Mock PaymentMapper mapper;
    @Mock PaymentRepository payments;
    @Mock ClientRepository clients;
    @Mock SessionRepository sessions;
    @Mock ClientSessionTrackingRepository tracking;
    @Mock AuthenticatedUserService authenticatedUser;
    @Mock AppointmentRepository appointments;
    @Mock GymRepository gyms;
    @Mock NotificationService notifications;
    PaymentServiceImpl service;

    @BeforeEach void setUp() { service = new PaymentServiceImpl(mapper, payments, clients, sessions, tracking, authenticatedUser, appointments, gyms, notifications); }

    @Test
    void managerHistoryUsesGlobalOrClientScopedQueries() {
        Payment first = Payment.builder().id(1).build();
        Payment second = Payment.builder().id(2).build();
        when(payments.findAllByOrderByPaymentDateDescIdDesc()).thenReturn(List.of(second, first));
        when(payments.findByClientIdOrderByPaymentDateDescIdDesc(9)).thenReturn(List.of(first));
        when(mapper.toDto(second)).thenReturn(new PaymentDTO());
        when(mapper.toDto(first)).thenReturn(new PaymentDTO());

        assertEquals(2, service.getAll(null).size());
        assertEquals(1, service.getAll(9).size());
    }

    @Test
    void clientHistoryDerivesClientIdFromAuthenticatedProfile() {
        when(authenticatedUser.client()).thenReturn(Client.builder().id(9).build());
        when(payments.findByClientIdOrderByPaymentDateDescIdDesc(9)).thenReturn(List.of());

        assertEquals(List.of(), service.getOwn());
        verify(payments).findByClientIdOrderByPaymentDateDescIdDesc(9);
        verify(payments, never()).findAllByOrderByPaymentDateDescIdDesc();
    }

    @Test
    void paymentCreationNotifiesClient() {
        Client client = Client.builder().id(9).build();
        Session session = new Session(); session.setId(2);
        PaymentDTO dto = new PaymentDTO(); dto.setPaidAppointments(4);
        when(clients.findById(9)).thenReturn(Optional.of(client));
        when(sessions.findById(2)).thenReturn(Optional.of(session));
        when(tracking.findByClientAndSession(client, session)).thenReturn(Optional.empty());
        when(payments.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(mapper.toDto(any(Payment.class))).thenReturn(dto);

        assertEquals(dto, service.create(new CreatePaymentRequest(9, 2, 4, LocalDate.now())));

        verify(notifications).sendPaymentConfirmationNotification(client, dto);
    }

    @Test
    void paymentStatusUsesOnlyHeldAppointmentsAndNeverReturnsNegativeDebt() {
        Session individual = new Session(); individual.setType(SessionType.INDIVIDUAL);
        Session group = new Session(); group.setType(SessionType.GROUP);
        when(authenticatedUser.client()).thenReturn(Client.builder().id(9).build());
        when(gyms.findFirstByOrderByIdAsc()).thenReturn(Optional.of(Gym.builder().timezone("Europe/Belgrade").build()));
        when(appointments.findDistinctByClientAppointmentsClientIdOrderByDateDescStartTimeDesc(9)).thenReturn(List.of(
                Appointment.builder().date(LocalDate.now().minusDays(1)).endTime(LocalTime.NOON).session(individual).build(),
                Appointment.builder().date(LocalDate.now().minusDays(2)).endTime(LocalTime.NOON).session(individual).build(),
                Appointment.builder().date(LocalDate.now().plusDays(1)).endTime(LocalTime.NOON).session(group).build()));
        when(payments.findByClientIdOrderByPaymentDateDescIdDesc(9)).thenReturn(List.of(
                Payment.builder().session(individual).paidAppointments(1).build(),
                Payment.builder().session(group).paidAppointments(5).build()));

        var result = service.getOwnStatus();

        assertEquals(2, result.size());
        assertEquals(1, result.get(0).owed());
        assertEquals(0, result.get(1).held());
        assertEquals(0, result.get(1).owed());
    }
}
