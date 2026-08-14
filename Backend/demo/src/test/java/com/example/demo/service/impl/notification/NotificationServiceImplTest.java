package com.example.demo.service.impl.notification;

import com.example.demo.dto.AppointmentDTO;
import com.example.demo.dto.PaymentDTO;
import com.example.demo.dto.SessionDTO;
import com.example.demo.enums.NotificationPreference;
import com.example.demo.enums.SessionType;
import com.example.demo.model.user.Client;
import com.example.demo.model.user.Trainer;
import com.example.demo.model.user.User;
import com.example.demo.repository.user.UserRepository;
import com.example.demo.service.notification.email.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDate;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Covers the NotificationPreference branching added/fixed in the notification-system audit
 * session (see AGENTS.md "Upgrade: notification decisions"): every per-recipient notification
 * method must send email-only on EMAIL, WebSocket-only on PUSH, and both on BOTH. Trainer
 * assignment (a) and client-upcoming-appointment (d) previously ignored the preference entirely
 * (always WebSocket) - this test exists specifically so that regression can't reappear silently,
 * and covers the same PUSH/EMAIL/BOTH matrix for the two new notification types added in this
 * session (payment confirmation, per-type).
 */
@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;
    @Mock
    private EmailService emailService;
    @Mock
    private UserRepository userRepository;

    private NotificationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new NotificationServiceImpl(messagingTemplate, emailService, userRepository);
    }

    private User userWithPreference(Integer id, NotificationPreference preference) {
        return User.builder().id(id).email("user" + id + "@test.dev").notificationPreference(preference).build();
    }

    private AppointmentDTO appointment() {
        AppointmentDTO dto = new AppointmentDTO();
        dto.setDate(LocalDate.of(2026, 8, 20));
        return dto;
    }

    private PaymentDTO payment() {
        PaymentDTO dto = new PaymentDTO();
        dto.setPaidAppointments(5);
        dto.setPaymentDate(LocalDate.of(2026, 8, 20));
        dto.setSession(new SessionDTO(1, SessionType.INDIVIDUAL, 1));
        return dto;
    }

    // --- (a) trainer assignment ---

    @Test
    void trainerAssignment_email_sendsEmailOnly() {
        User user = userWithPreference(10, NotificationPreference.EMAIL);
        Trainer trainer = Trainer.builder().id(1).user(user).build();
        when(userRepository.findById(10)).thenReturn(Optional.of(user));

        service.sendTrainerAssignmentNotification(trainer, appointment());

        verify(emailService).sendTrainerAssignmentEmail(eq(user.getEmail()), any());
        verify(messagingTemplate, never()).convertAndSend(anyString(), anyString());
    }

    @Test
    void trainerAssignment_push_sendsWebSocketOnly() {
        User user = userWithPreference(11, NotificationPreference.PUSH);
        Trainer trainer = Trainer.builder().id(2).user(user).build();
        when(userRepository.findById(11)).thenReturn(Optional.of(user));

        service.sendTrainerAssignmentNotification(trainer, appointment());

        verify(messagingTemplate).convertAndSend(eq("/topic/trainer2"), anyString());
        verify(emailService, never()).sendTrainerAssignmentEmail(anyString(), any());
    }

    @Test
    void trainerAssignment_both_sendsEmailAndWebSocket() {
        User user = userWithPreference(12, NotificationPreference.BOTH);
        Trainer trainer = Trainer.builder().id(3).user(user).build();
        when(userRepository.findById(12)).thenReturn(Optional.of(user));

        service.sendTrainerAssignmentNotification(trainer, appointment());

        verify(emailService).sendTrainerAssignmentEmail(eq(user.getEmail()), any());
        verify(messagingTemplate).convertAndSend(eq("/topic/trainer3"), anyString());
    }

    // --- (d) client upcoming appointment (1h-before sweep) ---

    @Test
    void clientUpcoming_email_sendsEmailOnly() {
        User user = userWithPreference(20, NotificationPreference.EMAIL);
        Client client = Client.builder().id(5).user(user).build();
        when(userRepository.findById(20)).thenReturn(Optional.of(user));

        service.sendClientUpcomingAppointmentNotification(client, appointment());

        verify(emailService).sendClientUpcomingAppointmentEmail(eq(user.getEmail()), any());
        verify(messagingTemplate, never()).convertAndSend(anyString(), anyString());
    }

    @Test
    void clientUpcoming_push_sendsWebSocketOnly() {
        User user = userWithPreference(21, NotificationPreference.PUSH);
        Client client = Client.builder().id(6).user(user).build();
        when(userRepository.findById(21)).thenReturn(Optional.of(user));

        service.sendClientUpcomingAppointmentNotification(client, appointment());

        verify(messagingTemplate).convertAndSend(eq("/topic/client6"), anyString());
        verify(emailService, never()).sendClientUpcomingAppointmentEmail(anyString(), any());
    }

    @Test
    void clientUpcoming_both_sendsEmailAndWebSocket() {
        User user = userWithPreference(22, NotificationPreference.BOTH);
        Client client = Client.builder().id(7).user(user).build();
        when(userRepository.findById(22)).thenReturn(Optional.of(user));

        service.sendClientUpcomingAppointmentNotification(client, appointment());

        verify(emailService).sendClientUpcomingAppointmentEmail(eq(user.getEmail()), any());
        verify(messagingTemplate).convertAndSend(eq("/topic/client7"), anyString());
    }

    // --- payment confirmation (new notification type) ---

    @Test
    void paymentConfirmation_email_sendsEmailOnly() {
        User user = userWithPreference(30, NotificationPreference.EMAIL);
        Client client = Client.builder().id(8).user(user).build();
        when(userRepository.findById(30)).thenReturn(Optional.of(user));

        service.sendPaymentConfirmationNotification(client, payment());

        verify(emailService).sendPaymentConfirmationEmail(eq(user.getEmail()), any());
        verify(messagingTemplate, never()).convertAndSend(anyString(), anyString());
    }

    @Test
    void paymentConfirmation_push_sendsWebSocketOnly() {
        User user = userWithPreference(31, NotificationPreference.PUSH);
        Client client = Client.builder().id(9).user(user).build();
        when(userRepository.findById(31)).thenReturn(Optional.of(user));

        service.sendPaymentConfirmationNotification(client, payment());

        verify(messagingTemplate).convertAndSend(eq("/topic/client9"), anyString());
        verify(emailService, never()).sendPaymentConfirmationEmail(anyString(), any());
    }

    @Test
    void paymentConfirmation_both_sendsEmailAndWebSocket() {
        User user = userWithPreference(32, NotificationPreference.BOTH);
        Client client = Client.builder().id(13).user(user).build();
        when(userRepository.findById(32)).thenReturn(Optional.of(user));

        service.sendPaymentConfirmationNotification(client, payment());

        verify(emailService).sendPaymentConfirmationEmail(eq(user.getEmail()), any());
        verify(messagingTemplate).convertAndSend(eq("/topic/client13"), anyString());
    }

    // --- manager alert broadcast (new notification type) ---

    @Test
    void managerAlert_alwaysBroadcastsToFixedTopic_noPreferenceLookup() {
        service.sendManagerAlert("Nova rezervacija: test@test.dev");

        verify(messagingTemplate).convertAndSend(eq("/topic/manager"), anyString());
        verify(userRepository, never()).findById(any());
    }
}
