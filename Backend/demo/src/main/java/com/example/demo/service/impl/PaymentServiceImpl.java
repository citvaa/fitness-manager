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
import com.example.demo.service.PaymentService;
import com.example.demo.service.params.request.user.client.CreatePaymentRequest;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
@RequiredArgsConstructor
@Service
public class PaymentServiceImpl implements PaymentService {

    private final PaymentMapper paymentMapper;
    private final PaymentRepository paymentRepository;
    private final ClientRepository clientRepository;
    private final SessionRepository sessionRepository;
    private final ClientSessionTrackingRepository clientSessionTrackingRepository;

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
}
