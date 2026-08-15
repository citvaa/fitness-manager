package com.example.demo.service;

import com.example.demo.dto.PaymentDTO;
import com.example.demo.service.params.request.user.client.CreatePaymentRequest;
import com.example.demo.service.params.response.payment.PaymentStatusResponse;

public interface PaymentService {
    PaymentDTO create(CreatePaymentRequest request);
    java.util.List<PaymentDTO> getAll(Integer clientId);
    java.util.List<PaymentDTO> getOwn();
    java.util.List<PaymentStatusResponse> getStatus(Integer clientId);
    java.util.List<PaymentStatusResponse> getOwnStatus();
}
