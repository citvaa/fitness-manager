package com.example.demo.service.impl;

import com.example.demo.dto.PaymentDTO;
import com.example.demo.mapper.PaymentMapper;
import com.example.demo.model.Payment;
import com.example.demo.model.user.Client;
import com.example.demo.repository.PaymentRepository;
import com.example.demo.repository.SessionRepository;
import com.example.demo.repository.user.ClientRepository;
import com.example.demo.repository.user.ClientSessionTrackingRepository;
import com.example.demo.service.security.AuthenticatedUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {
    @Mock PaymentMapper mapper;
    @Mock PaymentRepository payments;
    @Mock ClientRepository clients;
    @Mock SessionRepository sessions;
    @Mock ClientSessionTrackingRepository tracking;
    @Mock AuthenticatedUserService authenticatedUser;
    PaymentServiceImpl service;

    @BeforeEach void setUp() { service = new PaymentServiceImpl(mapper, payments, clients, sessions, tracking, authenticatedUser); }

    @Test
    void managerHistoryUsesGlobalOrClientScopedQueries() {
        Payment first = Payment.builder().id(1).build();
        Payment second = Payment.builder().id(2).build();
        when(payments.findAllByOrderByPaymentDateDescIdDesc()).thenReturn(List.of(second, first));
        when(payments.findByClientIdOrderByPaymentDateDescIdDesc(9)).thenReturn(List.of(first));
        when(mapper.toDto(second)).thenReturn(new PaymentDTO());
        when(mapper.toDto(first)).thenReturn(new PaymentDTO());

        assertEquals(2, service.getAll(null).size());
        assertEquals(1, service.getAll(9).size());
    }

    @Test
    void clientHistoryDerivesClientIdFromAuthenticatedProfile() {
        when(authenticatedUser.client()).thenReturn(Client.builder().id(9).build());
        when(payments.findByClientIdOrderByPaymentDateDescIdDesc(9)).thenReturn(List.of());

        assertEquals(List.of(), service.getOwn());
        verify(payments).findByClientIdOrderByPaymentDateDescIdDesc(9);
        verify(payments, never()).findAllByOrderByPaymentDateDescIdDesc();
    }
}
