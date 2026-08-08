package com.example.demo.controller;

import com.example.demo.annotation.RoleRequired;
import com.example.demo.dto.PaymentDTO;
import com.example.demo.service.PaymentService;
import com.example.demo.service.params.request.user.client.CreatePaymentRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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

    /** MANAGER-facing payment history, optionally filtered to one client. */
    @RoleRequired("MANAGER")
    @GetMapping
    public ResponseEntity<List<PaymentDTO>> getAll(@RequestParam(required = false) Integer clientId) {
        return ResponseEntity.ok(paymentService.getAll(clientId));
    }

    /** CLIENT self-service - own payment history, resolved from the JWT. */
    @RoleRequired("CLIENT")
    @GetMapping("/me")
    public ResponseEntity<List<PaymentDTO>> getMyPayments() {
        return ResponseEntity.ok(paymentService.getMyPayments());
    }
}
