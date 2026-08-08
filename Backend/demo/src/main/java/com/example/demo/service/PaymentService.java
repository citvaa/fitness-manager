package com.example.demo.service;

import com.example.demo.dto.PaymentDTO;
import com.example.demo.service.params.request.user.client.CreatePaymentRequest;

import java.util.List;

public interface PaymentService {
    PaymentDTO create(CreatePaymentRequest request);

    /** MANAGER-facing - all payments, optionally filtered to one client, newest first. */
    List<PaymentDTO> getAll(Integer clientId);

    /** CLIENT-facing self-service - the logged-in client's own payments (resolved from the JWT). */
    List<PaymentDTO> getMyPayments();
}
