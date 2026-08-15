package com.example.demo.controller;

import com.example.demo.annotation.RoleRequired;
import com.example.demo.dto.AppointmentDTO;
import com.example.demo.dto.SessionDTO;
import com.example.demo.service.AppointmentService;
import com.example.demo.service.params.request.appointment.CreateAppointmentRequest;
import com.example.demo.service.params.response.appointment.RecurringAppointmentResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;
import java.util.Set;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/appointment")
public class AppointmentController {

    private final AppointmentService appointmentService;

    @RoleRequired("MANAGER")
    @PostMapping
    public ResponseEntity<AppointmentDTO> create(@Valid @RequestBody CreateAppointmentRequest request) throws JsonProcessingException {
        AppointmentDTO createdAppointment = appointmentService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdAppointment);
    }

    @RoleRequired("MANAGER")
    @GetMapping("/sessions")
    public List<SessionDTO> getSessions() { return appointmentService.getSessions(); }

    @RoleRequired("MANAGER")
    @PostMapping("/{appointmentId}/add-trainer")
    public ResponseEntity<AppointmentDTO> addTrainer(@PathVariable Integer appointmentId, @RequestParam Integer trainerId) {
        AppointmentDTO updatedAppointment = appointmentService.addTrainer(appointmentId, trainerId);
        return ResponseEntity.ok(updatedAppointment);
    }

    @RoleRequired("MANAGER")
    @DeleteMapping("/{id}/remove-trainer")
    public ResponseEntity<AppointmentDTO> removeTrainer(@PathVariable Integer id) {
        AppointmentDTO updatedAppointment = appointmentService.removeTrainer(id);
        return ResponseEntity.ok(updatedAppointment);
    }

    @RoleRequired("MANAGER")
    @PostMapping("/{appointmentId}/add-clients")
    public ResponseEntity<AppointmentDTO> addClients(@PathVariable Integer appointmentId, @RequestParam Set<Integer> clientIds) {
        AppointmentDTO updatedAppointment = appointmentService.addClients(appointmentId, clientIds);
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

    @RoleRequired("CLIENT")
    @PostMapping("/{id}/reserve")
    public ResponseEntity<AppointmentDTO> reserve(@PathVariable Integer id) {
        return ResponseEntity.ok(appointmentService.reserve(id));
    }

    @RoleRequired("CLIENT")
    @DeleteMapping("/{id}/cancel")
    public ResponseEntity<AppointmentDTO> cancel(@PathVariable Integer id) {
        return ResponseEntity.ok(appointmentService.cancel(id));
    }

    @RoleRequired({"MANAGER", "TRAINER"})
    @GetMapping("/without-trainer")
    public ResponseEntity<List<AppointmentDTO>> getAllWithoutTrainer() {
        return ResponseEntity.ok(appointmentService.getAllWithoutTrainer());
    }

    @RoleRequired("TRAINER")
    @PostMapping("/{id}/assign")
    public ResponseEntity<AppointmentDTO> assign(@PathVariable Integer id) {
        return ResponseEntity.ok(appointmentService.assign(id));
    }

    @RoleRequired("TRAINER")
    @DeleteMapping("/{id}/unassign")
    public ResponseEntity<AppointmentDTO> unassign(@PathVariable Integer id) {
        return ResponseEntity.ok(appointmentService.unassign(id));
    }

    @RoleRequired({"TRAINER", "CLIENT"})
    @GetMapping("/me")
    public ResponseEntity<List<AppointmentDTO>> getMine() {
        return ResponseEntity.ok(appointmentService.getMyAppointments());
    }

    @RoleRequired("MANAGER")
    @PostMapping("/recurring")
    public RecurringAppointmentResponse createRecurring(@Valid @RequestBody CreateAppointmentRequest request) {
        return appointmentService.createRecurring(request);
    }

    @RoleRequired("TRAINER") @GetMapping("/me/today-upcoming")
    public List<AppointmentDTO> getMyUpcomingToday() { return appointmentService.getMyUpcomingToday(); }

    @RoleRequired("MANAGER")
    @GetMapping("/date/{date}")
    public List<AppointmentDTO> getForDate(@PathVariable java.time.LocalDate date) {
        return appointmentService.getAppointmentsForDate(date);
    }
}
