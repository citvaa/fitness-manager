package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class PaymentDTO {
    private Integer id;
    private ClientDTO client;
    private SessionDTO session;
    private Integer paidSessions;
    private LocalDate paymentDate;
}
