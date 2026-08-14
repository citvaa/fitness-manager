package com.example.demo.controller;

import com.example.demo.annotation.RoleRequired;
import com.example.demo.dto.AppointmentDTO;
import com.example.demo.dto.gym.RoomDTO;
import com.example.demo.dto.user.TrainerDTO;
import com.example.demo.service.AppointmentService;
import com.example.demo.service.params.request.appointment.CreateAppointmentRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/appointment")
public class AppointmentController {

    private final AppointmentService appointmentService;

    @RoleRequired("MANAGER")
    @PostMapping
    public ResponseEntity<AppointmentDTO> create(@RequestBody CreateAppointmentRequest request) throws JsonProcessingException {
        AppointmentDTO createdAppointment = appointmentService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdAppointment);
    }

    @RoleRequired("MANAGER")
    @PostMapping("/recurring")
    public ResponseEntity<List<AppointmentDTO>> createRecurring(@RequestBody CreateAppointmentRequest request) throws JsonProcessingException {
        List<AppointmentDTO> created = appointmentService.createRecurringWeekly(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

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

    @RoleRequired("MANAGER")
    @GetMapping
    public ResponseEntity<List<AppointmentDTO>> getAll() {
        return ResponseEntity.ok(appointmentService.getAll());
    }

    /** Backs the "new appointment" form's trainer picker - narrows the dropdown to trainers who
     * would actually pass create()'s validation for this date/time, so a manager can't even select
     * a doomed combination. UX narrowing only; create() re-validates from scratch regardless (see
     * AGENTS.md "Upgrade: appointment picker filtering decisions" for why this is not a substitute
     * for that check - e.g. a concurrent booking between listing and submit). */
    @RoleRequired("MANAGER")
    @GetMapping("/available-trainers")
    public ResponseEntity<List<TrainerDTO>> getAvailableTrainers(
            @RequestParam LocalDate date, @RequestParam LocalTime startTime, @RequestParam LocalTime endTime) {
        return ResponseEntity.ok(appointmentService.getAvailableTrainers(date, startTime, endTime));
    }

    /** Same reasoning as getAvailableTrainers() above, for the room picker. */
    @RoleRequired("MANAGER")
    @GetMapping("/available-rooms")
    public ResponseEntity<List<RoomDTO>> getAvailableRooms(
            @RequestParam LocalDate date, @RequestParam LocalTime startTime, @RequestParam LocalTime endTime) {
        return ResponseEntity.ok(appointmentService.getAvailableRooms(date, startTime, endTime));
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

    @RoleRequired("CLIENT")
    @GetMapping("/me")
    public ResponseEntity<List<AppointmentDTO>> getMyAppointmentsAsClient() {
        return ResponseEntity.ok(appointmentService.getMyAppointmentsAsClient());
    }

    @RoleRequired("TRAINER")
    @GetMapping("/trainer/me")
    public ResponseEntity<List<AppointmentDTO>> getMyAppointmentsAsTrainer() {
        return ResponseEntity.ok(appointmentService.getMyAppointmentsAsTrainer());
    }
}
