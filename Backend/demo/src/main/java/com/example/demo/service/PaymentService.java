package com.example.demo.service;

import com.example.demo.dto.PaymentDTO;
import com.example.demo.service.params.request.user.client.CreatePaymentRequest;

public interface PaymentService {
    PaymentDTO create(CreatePaymentRequest request);
}
