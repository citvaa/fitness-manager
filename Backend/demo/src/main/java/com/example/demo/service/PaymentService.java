package com.example.demo.service;

import com.example.demo.dto.PaymentDTO;
import com.example.demo.dto.SessionTypePaymentStatusDTO;
import com.example.demo.service.params.request.user.client.CreatePaymentRequest;

import java.util.List;

public interface PaymentService {
    PaymentDTO create(CreatePaymentRequest request);

    /** MANAGER-facing - all payments, optionally filtered to one client, newest first. */
    List<PaymentDTO> getAll(Integer clientId);

    /** CLIENT-facing self-service - the logged-in client's own payments (resolved from the JWT). */
    List<PaymentDTO> getMyPayments();

    /** MANAGER-facing - per-SessionType comparison of a client's actually-held past appointments
     * vs what they've paid for (see AGENTS.md "Upgrade: payment debt tracking decisions"). One
     * entry per {@link com.example.demo.enums.SessionType}, always both values present (0 if
     * none). */
    List<SessionTypePaymentStatusDTO> getPaymentStatus(Integer clientId);

    /** CLIENT-facing self-service version of {@link #getPaymentStatus(Integer)}, resolved from
     * the JWT. */
    List<SessionTypePaymentStatusDTO> getMyPaymentStatus();
}
