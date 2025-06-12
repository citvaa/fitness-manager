package com.example.demo.dto;

import com.example.demo.dto.summary.ClientSummaryDTO;
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
    private ClientSummaryDTO client;
    private SessionDTO session;
    private Integer paidAppointments;
    private LocalDate paymentDate;
}
