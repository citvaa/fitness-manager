package com.example.demo.service.impl;

import com.example.demo.dto.PaymentDTO;
import com.example.demo.mapper.PaymentMapper;
import com.example.demo.model.Client;
import com.example.demo.model.Payment;
import com.example.demo.model.Session;
import com.example.demo.repository.ClientRepository;
import com.example.demo.repository.PaymentRepository;
import com.example.demo.repository.SessionRepository;
import com.example.demo.service.PaymentService;
import com.example.demo.service.params.request.Client.CreatePaymentRequest;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class PaymentServiceImpl implements PaymentService {

    private final PaymentMapper paymentMapper;
    private final PaymentRepository paymentRepository;
    private final ClientRepository clientRepository;
    private final SessionRepository sessionRepository;

    @Override
    public PaymentDTO create(@NotNull CreatePaymentRequest request) {
        if (request.getPaidSessions() <= 0) {
            throw new IllegalArgumentException("Paid sessions must be greater than zero");
        }

        Client client = clientRepository.findById(request.getClientId())
                .orElseThrow(() -> new IllegalArgumentException("Client not found"));

        client.setRemainingSessions(client.getRemainingSessions() + request.getPaidSessions());
        clientRepository.save(client);

        Session session = sessionRepository.findById(request.getSessionId())
                .orElseThrow(() -> new IllegalArgumentException("Session not found"));

        Payment payment = new Payment();
        payment.setClient(client);
        payment.setSession(session);
        payment.setPaidSessions(request.getPaidSessions());
        payment.setPaymentDate(request.getPaymentDate());

        return paymentMapper.toDto(paymentRepository.save(payment));
    }
}
