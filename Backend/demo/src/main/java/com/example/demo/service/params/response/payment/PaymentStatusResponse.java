package com.example.demo.service.params.response.payment;

import com.example.demo.enums.SessionType;

public record PaymentStatusResponse(SessionType type, int held, int paid, int owed) {
}
