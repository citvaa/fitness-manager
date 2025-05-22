package com.example.demo.service;

import com.example.demo.dto.PaymentDTO;
import com.example.demo.service.params.request.Client.CreatePaymentRequest;

public interface PaymentService {
    PaymentDTO create(CreatePaymentRequest request);
}
