package com.example.demo.controller;

import com.example.demo.annotation.RoleRequired;
import com.example.demo.dto.PaymentDTO;
import com.example.demo.service.PaymentService;
import com.example.demo.service.params.request.user.client.CreatePaymentRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/payment")
public class PaymentController {

    private final PaymentService paymentService;

    @RoleRequired("MANAGER")
    @PostMapping
    public ResponseEntity<PaymentDTO> create(@RequestBody CreatePaymentRequest request) {
        PaymentDTO paymentDTO = paymentService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentDTO);
    }
}
