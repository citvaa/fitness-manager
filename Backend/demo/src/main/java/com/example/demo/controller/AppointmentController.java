package com.example.demo.controller;

import com.example.demo.annotation.RoleRequired;
import com.example.demo.dto.AppointmentDTO;
import com.example.demo.service.AppointmentService;
import com.example.demo.service.params.request.Appointment.CreateAppointmentRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/appointment")
public class AppointmentController {

    private final AppointmentService appointmentServiceImpl;

    @RoleRequired("MANAGER")
    @PostMapping
    public ResponseEntity<AppointmentDTO> create(@RequestBody CreateAppointmentRequest request) {
        AppointmentDTO createdAppointment = appointmentServiceImpl.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdAppointment);
    }
}
