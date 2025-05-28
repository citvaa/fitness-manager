package com.example.demo.controller;

import com.example.demo.annotation.RoleRequired;
import com.example.demo.dto.AppointmentDTO;
import com.example.demo.service.AppointmentService;
import com.example.demo.service.params.request.Appointment.CreateAppointmentRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

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

    @RoleRequired("MANAGER")
    @PostMapping("/{appointmentId}/add-trainer")
    public ResponseEntity<AppointmentDTO> addTrainer(@PathVariable Integer appointmentId, @RequestParam Integer trainerId) {
        AppointmentDTO updatedAppointment = appointmentServiceImpl.addTrainer(appointmentId, trainerId);
        return ResponseEntity.ok(updatedAppointment);
    }

    @RoleRequired("MANAGER")
    @PostMapping("/{appointmentId}/add-clients")
    public ResponseEntity<AppointmentDTO> addClient(@PathVariable Integer appointmentId, @RequestParam Set<Integer> clientIds) {
        AppointmentDTO updatedAppointment = appointmentServiceImpl.addClients(appointmentId, clientIds);
        return ResponseEntity.ok(updatedAppointment);
    }

    @RoleRequired("MANAGER")
    @DeleteMapping("/{id}/remove-trainer")
    public ResponseEntity<AppointmentDTO> removeTrainer(@PathVariable Integer id) {
        AppointmentDTO updatedAppointment = appointmentServiceImpl.removeTrainer(id);
        return ResponseEntity.ok(updatedAppointment);
    }

    @RoleRequired("MANAGER")
    @DeleteMapping("/{appointmentId}/remove-client")
    public ResponseEntity<AppointmentDTO> removeClient(@PathVariable Integer appointmentId, @RequestParam Integer clientId) {
        AppointmentDTO updatedAppointment = appointmentServiceImpl.removeClient(appointmentId, clientId);
        return ResponseEntity.ok(updatedAppointment);
    }
}
