package com.example.demo.service.params.request.Client;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class CreatePaymentRequest {
    private Integer clientId;
    private Integer sessionId;
    private Integer paidSessions;
    private LocalDate paymentDate;
}
