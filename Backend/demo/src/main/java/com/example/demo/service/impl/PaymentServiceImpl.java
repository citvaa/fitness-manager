package com.example.demo.service.impl;

import com.example.demo.dto.PaymentDTO;
import com.example.demo.mapper.PaymentMapper;
import com.example.demo.model.user.Client;
import com.example.demo.model.user.ClientSessionTracking;
import com.example.demo.model.Payment;
import com.example.demo.model.Session;
import com.example.demo.repository.user.ClientRepository;
import com.example.demo.repository.user.ClientSessionTrackingRepository;
import com.example.demo.repository.PaymentRepository;
import com.example.demo.repository.SessionRepository;
import com.example.demo.repository.AppointmentRepository;
import com.example.demo.repository.gym.GymRepository;
import com.example.demo.enums.SessionType;
import com.example.demo.service.PaymentService;
import com.example.demo.service.security.AuthenticatedUserService;
import com.example.demo.service.notification.NotificationService;
import com.example.demo.service.params.request.user.client.CreatePaymentRequest;
import com.example.demo.service.params.response.payment.PaymentStatusResponse;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.EnumMap;

@Transactional(readOnly = true)
@RequiredArgsConstructor
@Service
public class PaymentServiceImpl implements PaymentService {

    private final PaymentMapper paymentMapper;
    private final PaymentRepository paymentRepository;
    private final ClientRepository clientRepository;
    private final SessionRepository sessionRepository;
    private final ClientSessionTrackingRepository clientSessionTrackingRepository;
    private final AuthenticatedUserService authenticatedUserService;
    private final AppointmentRepository appointmentRepository;
    private final GymRepository gymRepository;
    private final NotificationService notificationService;

    public java.util.List<PaymentDTO> getAll(Integer clientId) {
        java.util.List<Payment> payments = clientId == null
                ? paymentRepository.findAllByOrderByPaymentDateDescIdDesc()
                : paymentRepository.findByClientIdOrderByPaymentDateDescIdDesc(clientId);
        return payments.stream().map(paymentMapper::toDto).toList();
    }

    public java.util.List<PaymentDTO> getOwn() {
        return getAll(authenticatedUserService.client().getId());
    }

    public java.util.List<PaymentStatusResponse> getStatus(Integer clientId) {
        fetchClient(clientId);
        return computePaymentStatus(clientId);
    }

    public java.util.List<PaymentStatusResponse> getOwnStatus() {
        return computePaymentStatus(authenticatedUserService.client().getId());
    }

    @Transactional
    public PaymentDTO create(@NotNull CreatePaymentRequest request) {
        validatePaymentRequest(request);

        Client client = fetchClient(request.getClientId());
        Session session = fetchSession(request.getSessionId());

        ClientSessionTracking tracking = getOrCreateClientSessionTracking(client, session);
        updateClientSessionTracking(tracking, request.getPaidAppointments());

        Payment payment = createPayment(client, session, request);
        PaymentDTO paymentDTO = paymentMapper.toDto(paymentRepository.save(payment));
        notificationService.sendPaymentConfirmationNotification(client, paymentDTO);
        return paymentDTO;
    }




    private void validatePaymentRequest(@NotNull CreatePaymentRequest request) {
        if (request.getPaidAppointments() <= 0) {
            throw new IllegalArgumentException("Paid sessions must be greater than zero");
        }
    }

    private Client fetchClient(Integer clientId) {
        return clientRepository.findById(clientId)
                .orElseThrow(() -> new IllegalArgumentException("Client not found"));
    }

    private Session fetchSession(Integer sessionId) {
        return sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found"));
    }

    private ClientSessionTracking getOrCreateClientSessionTracking(Client client, Session session) {
        return clientSessionTrackingRepository.findByClientAndSession(client, session)
                .orElseGet(() -> ClientSessionTracking.builder()
                        .client(client)
                        .session(session)
                        .remainingAppointments(0)
                        .reservedAppointments(0)
                        .build());
    }

    private void updateClientSessionTracking(@NotNull ClientSessionTracking tracking, Integer paidAppointments) {
        tracking.setRemainingAppointments(tracking.getRemainingAppointments() + paidAppointments);
        clientSessionTrackingRepository.save(tracking);
    }

    private Payment createPayment(Client client, Session session, @NotNull CreatePaymentRequest request) {
        return Payment.builder()
                .client(client)
                .session(session)
                .paidAppointments(request.getPaidAppointments())
                .paymentDate(request.getPaymentDate())
                .build();
    }

    private java.util.List<PaymentStatusResponse> computePaymentStatus(Integer clientId) {
        var held = new EnumMap<SessionType, Integer>(SessionType.class);
        var paid = new EnumMap<SessionType, Integer>(SessionType.class);
        var gym = gymRepository.findFirstByOrderByIdAsc()
                .orElseThrow(() -> new IllegalStateException("Gym configuration not found"));
        var now = LocalDateTime.now(ZoneId.of(gym.getTimezone()));
        appointmentRepository.findDistinctByClientAppointmentsClientIdOrderByDateDescStartTimeDesc(clientId).stream()
                .filter(appointment -> LocalDateTime.of(appointment.getDate(), appointment.getEndTime()).isBefore(now))
                .forEach(appointment -> held.merge(appointment.getSession().getType(), 1, Integer::sum));
        paymentRepository.findByClientIdOrderByPaymentDateDescIdDesc(clientId)
                .forEach(payment -> paid.merge(payment.getSession().getType(), payment.getPaidAppointments(), Integer::sum));
        return java.util.Arrays.stream(SessionType.values()).map(type -> {
            int heldCount = held.getOrDefault(type, 0);
            int paidCount = paid.getOrDefault(type, 0);
            return new PaymentStatusResponse(type, heldCount, paidCount, Math.max(0, heldCount - paidCount));
        }).toList();
    }
}
