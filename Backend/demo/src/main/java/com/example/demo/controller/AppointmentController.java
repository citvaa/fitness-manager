package com.example.demo.controller;

import com.example.demo.annotation.RoleRequired;
import com.example.demo.dto.AppointmentDTO;
import com.example.demo.service.AppointmentService;
import com.example.demo.service.params.request.Appointment.CreateAppointmentRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/appointment")
public class AppointmentController {

    private final AppointmentService appointmentService;

    @RoleRequired("MANAGER")
    @PostMapping
    public ResponseEntity<AppointmentDTO> create(@RequestBody CreateAppointmentRequest request) {
        AppointmentDTO createdAppointment = appointmentService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdAppointment);
    }

    @RoleRequired({"MANAGER", "TRAINER"})
    @PostMapping("/{appointmentId}/add-trainer")
    public ResponseEntity<AppointmentDTO> addTrainer(@PathVariable Integer appointmentId, @RequestParam Integer trainerId) {
        AppointmentDTO updatedAppointment = appointmentService.addTrainer(appointmentId, trainerId);
        return ResponseEntity.ok(updatedAppointment);
    }

    @RoleRequired("MANAGER")
    @PostMapping("/{appointmentId}/add-clients")
    public ResponseEntity<AppointmentDTO> addClients(@PathVariable Integer appointmentId, @RequestParam Set<Integer> clientIds) {
        AppointmentDTO updatedAppointment = appointmentService.addClients(appointmentId, clientIds);
        return ResponseEntity.ok(updatedAppointment);
    }

    @RoleRequired("MANAGER")
    @DeleteMapping("/{id}/remove-trainer")
    public ResponseEntity<AppointmentDTO> removeTrainer(@PathVariable Integer id) {
        AppointmentDTO updatedAppointment = appointmentService.removeTrainer(id);
        return ResponseEntity.ok(updatedAppointment);
    }

    @RoleRequired("MANAGER")
    @DeleteMapping("/{appointmentId}/remove-client")
    public ResponseEntity<AppointmentDTO> removeClient(@PathVariable Integer appointmentId, @RequestParam Integer clientId) {
        AppointmentDTO updatedAppointment = appointmentService.removeClient(appointmentId, clientId);
        return ResponseEntity.ok(updatedAppointment);
    }

    @RoleRequired({"MANAGER", "CLIENT"})
    @GetMapping("/available")
    public ResponseEntity<List<AppointmentDTO>> getAvailable() {
        return ResponseEntity.ok(appointmentService.getAvailable());
    }

    @PostMapping("/{appointmentId}/add-client")
    public ResponseEntity<AppointmentDTO> reserve(@PathVariable Integer appointmentId, @RequestParam Integer clientId) {
        return ResponseEntity.ok(appointmentService.addClient(appointmentId, clientId));
    }

    @RoleRequired({"MANAGER", "TRAINER"})
    @GetMapping("/without-trainer")
    public ResponseEntity<List<AppointmentDTO>> getAllWithoutTrainer() {
        return ResponseEntity.ok(appointmentService.getAllWithoutTrainer());
    }

    @RoleRequired({"MANAGER", "CLIENT"})
    @DeleteMapping("/{appointmentId}/cancel")
    public ResponseEntity<AppointmentDTO> cancel(@PathVariable Integer appointmentId, @RequestParam Integer clientId) {
        return ResponseEntity.ok(appointmentService.cancel(appointmentId, clientId));
    }
}
