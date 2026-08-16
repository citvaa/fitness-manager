package com.example.demo.controller.schedule;

import com.example.demo.annotation.RoleRequired;
import com.example.demo.dto.schedule.TrainerScheduleDTO;
import com.example.demo.service.schedule.TrainerScheduleService;
import com.example.demo.service.params.request.schedule.CreateOwnTrainerScheduleRequest;
import com.example.demo.service.params.request.schedule.CreateOwnTrainerUnavailabilityRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/schedule/trainer")
public class TrainerScheduleController {

    private final TrainerScheduleService trainerScheduleService;

    /** MANAGER oversight - view any trainer's schedule. Read-only: writing to a trainer's own
     * schedule is TRAINER self-service only (POST .../me, .../me/recurring, .../me/unavailable
     * below) - see AGENTS.md "Upgrade: manager schedule-write removal decisions". */
    @RoleRequired("MANAGER")
    @GetMapping("/{trainerId}")
    public ResponseEntity<List<TrainerScheduleDTO>> getSchedule(@PathVariable Integer trainerId) {
        return ResponseEntity.ok(trainerScheduleService.getSchedule(trainerId));
    }

    /** TRAINER self-service - view own schedule (resolved from the JWT, not a path parameter). */
    @RoleRequired("TRAINER")
    @GetMapping("/me")
    public ResponseEntity<List<TrainerScheduleDTO>> getMySchedule() {
        return ResponseEntity.ok(trainerScheduleService.getMySchedule());
    }

    /** TRAINER self-service - create a working-hours entry for the logged-in trainer only. */
    @RoleRequired("TRAINER")
    @PostMapping("/me")
    public ResponseEntity<TrainerScheduleDTO> createMySchedule(@RequestBody CreateOwnTrainerScheduleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(trainerScheduleService.createMySchedule(request));
    }

    /** TRAINER self-service - "fiksni raspored": generates weekly-repeating WORKING shifts for the
     * logged-in trainer, same convention as POST /api/appointment/recurring. See AGENTS.md
     * "Upgrade: trainer fixed-schedule decisions". */
    @RoleRequired("TRAINER")
    @PostMapping("/me/recurring")
    public ResponseEntity<List<TrainerScheduleDTO>> createMyScheduleRecurring(@RequestBody CreateOwnTrainerScheduleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(trainerScheduleService.createMyScheduleRecurring(request));
    }

    /** TRAINER self-service - create an unavailability period for the logged-in trainer only. */
    @RoleRequired("TRAINER")
    @PostMapping("/me/unavailable")
    public ResponseEntity<Void> createMyUnavailability(@RequestBody CreateOwnTrainerUnavailabilityRequest request) {
        trainerScheduleService.createMyUnavailability(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * TRAINER self-service - a trainer may only delete their own entries, enforced in
     * TrainerScheduleServiceImpl.deleteSchedule (throws AccessDeniedException -> 403 otherwise).
     * MANAGER no longer has a bypass here - see AGENTS.md "Upgrade: manager schedule-write removal
     * decisions".
     */
    @RoleRequired("TRAINER")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSchedule(@PathVariable Integer id) {
        trainerScheduleService.deleteSchedule(id);
        return ResponseEntity.noContent().build();
    }
}
