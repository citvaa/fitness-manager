package com.example.demo.service.impl;

import com.example.demo.dto.PaymentDTO;
import com.example.demo.dto.SessionTypePaymentStatusDTO;
import com.example.demo.enums.SessionType;
import com.example.demo.mapper.PaymentMapper;
import com.example.demo.model.Appointment;
import com.example.demo.model.user.Client;
import com.example.demo.model.user.ClientSessionTracking;
import com.example.demo.model.Payment;
import com.example.demo.model.Session;
import com.example.demo.repository.AppointmentRepository;
import com.example.demo.repository.user.ClientRepository;
import com.example.demo.repository.user.ClientSessionTrackingRepository;
import com.example.demo.repository.PaymentRepository;
import com.example.demo.repository.SessionRepository;
import com.example.demo.service.PaymentService;
import com.example.demo.service.params.request.user.client.CreatePaymentRequest;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Transactional(readOnly = true)
@RequiredArgsConstructor
@Service
public class PaymentServiceImpl implements PaymentService {

    private final PaymentMapper paymentMapper;
    private final PaymentRepository paymentRepository;
    private final ClientRepository clientRepository;
    private final SessionRepository sessionRepository;
    private final ClientSessionTrackingRepository clientSessionTrackingRepository;
    private final AppointmentRepository appointmentRepository;

    @Transactional
    public PaymentDTO create(@NotNull CreatePaymentRequest request) {
        validatePaymentRequest(request);

        Client client = fetchClient(request.getClientId());
        Session session = fetchSession(request.getSessionId());

        ClientSessionTracking tracking = getOrCreateClientSessionTracking(client, session);
        updateClientSessionTracking(tracking, request.getPaidAppointments());

        Payment payment = createPayment(client, session, request);
        return paymentMapper.toDto(paymentRepository.save(payment));
    }

    public List<PaymentDTO> getAll(Integer clientId) {
        if (clientId != null) {
            return paymentMapper.toDto(paymentRepository.findByClientIdOrderByPaymentDateDesc(clientId));
        }
        return paymentMapper.toDto(paymentRepository.findAllByOrderByPaymentDateDesc());
    }

    public List<PaymentDTO> getMyPayments() {
        Client client = getAuthenticatedClient();
        return paymentMapper.toDto(paymentRepository.findByClientIdOrderByPaymentDateDesc(client.getId()));
    }

    @Override
    public List<SessionTypePaymentStatusDTO> getPaymentStatus(Integer clientId) {
        return computePaymentStatus(clientId);
    }

    @Override
    public List<SessionTypePaymentStatusDTO> getMyPaymentStatus() {
        Client client = getAuthenticatedClient();
        return computePaymentStatus(client.getId());
    }

    /** Compares actually-HELD past appointments (a client's own {@code ClientAppointment} rows
     * whose appointment has already ended, grouped by {@code Session.type}) against PAID
     * appointments ({@code Payment.paidAppointments}, same grouping) - see AGENTS.md "Upgrade:
     * payment debt tracking decisions" for why this queries real Appointment/ClientAppointment/
     * Payment data rather than {@code ClientSessionTracking} (whose `reservedAppointments`
     * includes future, not-yet-attended bookings - a client shouldn't show as "owing" for a
     * session that hasn't happened yet) or {@code DevDataSeeder}'s internal `bookedCounts` map
     * (seed-time only, not available at request time). Deliberately computed in Java over the
     * fetched lists rather than a grouped JPQL aggregation - the per-client row counts here are
     * small (a client's own appointment/payment history, not a table scan), and this stays
     * consistent with the same "small duplication/directness over cleverness" style already used
     * elsewhere in this service. */
    private List<SessionTypePaymentStatusDTO> computePaymentStatus(Integer clientId) {
        LocalDateTime now = LocalDateTime.now();
        Map<SessionType, Integer> held = new EnumMap<>(SessionType.class);
        for (Appointment appointment : appointmentRepository.findByClientAppointmentsClientIdOrderByDateDescStartTimeDesc(clientId)) {
            LocalDateTime end = LocalDateTime.of(appointment.getDate(), appointment.getEndTime());
            if (end.isBefore(now)) {
                held.merge(appointment.getSession().getType(), 1, Integer::sum);
            }
        }

        Map<SessionType, Integer> paid = new EnumMap<>(SessionType.class);
        for (Payment payment : paymentRepository.findByClientIdOrderByPaymentDateDesc(clientId)) {
            paid.merge(payment.getSession().getType(), payment.getPaidAppointments(), Integer::sum);
        }

        List<SessionTypePaymentStatusDTO> status = new ArrayList<>();
        for (SessionType type : SessionType.values()) {
            int heldCount = held.getOrDefault(type, 0);
            int paidCount = paid.getOrDefault(type, 0);
            status.add(new SessionTypePaymentStatusDTO(type, heldCount, paidCount, Math.max(0, heldCount - paidCount)));
        }
        return status;
    }

    /** Same JWT->email->repository idiom used across the codebase (see AGENTS.md, "Upgrade: service layer decisions"). */
    private Client getAuthenticatedClient() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new AccessDeniedException("Neovlašćen pristup!");
        }
        String email = jwt.getClaim("email");
        return clientRepository.findByUserEmail(email)
                .orElseThrow(() -> new RuntimeException("Klijent nije pronađen za prijavljenog korisnika!"));
    }

    private void validatePaymentRequest(@NotNull CreatePaymentRequest request) {
        if (request.getPaidAppointments() <= 0) {
            throw new IllegalArgumentException("Broj plaćenih termina mora biti veći od nule");
        }
    }

    private Client fetchClient(Integer clientId) {
        return clientRepository.findById(clientId)
                .orElseThrow(() -> new IllegalArgumentException("Klijent nije pronađen"));
    }

    private Session fetchSession(Integer sessionId) {
        return sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Sesija nije pronađena"));
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
}
