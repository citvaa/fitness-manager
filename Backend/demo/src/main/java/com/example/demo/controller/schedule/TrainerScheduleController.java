package com.example.demo.controller.schedule;

import com.example.demo.annotation.RoleRequired;
import com.example.demo.dto.schedule.TrainerScheduleDTO;
import com.example.demo.service.schedule.TrainerScheduleService;
import com.example.demo.service.params.request.schedule.CreateOwnTrainerScheduleRequest;
import com.example.demo.service.params.request.schedule.CreateOwnTrainerUnavailabilityRequest;
import com.example.demo.service.params.request.schedule.CreateTrainerScheduleRequest;
import com.example.demo.service.params.request.schedule.CreateTrainerUnavailabilityRequest;
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

    @RoleRequired("MANAGER")
    @PostMapping
    public ResponseEntity<TrainerScheduleDTO> createSchedule(@RequestBody CreateTrainerScheduleRequest request) {
        TrainerScheduleDTO trainerScheduleDTO = trainerScheduleService.createSchedule(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(trainerScheduleDTO);
    }

    @RoleRequired("MANAGER")
    @PostMapping("/unavailable")
    public ResponseEntity<Void> createUnavailability(@RequestBody CreateTrainerUnavailabilityRequest request) {
        trainerScheduleService.createUnavailability(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /** MANAGER oversight - view any trainer's schedule. See AGENTS.md "Upgrade: Faza 6 decisions". */
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

    /** TRAINER self-service - create an unavailability period for the logged-in trainer only. */
    @RoleRequired("TRAINER")
    @PostMapping("/me/unavailable")
    public ResponseEntity<Void> createMyUnavailability(@RequestBody CreateOwnTrainerUnavailabilityRequest request) {
        trainerScheduleService.createMyUnavailability(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * MANAGER may delete any entry; TRAINER may only delete their own - enforced in
     * TrainerScheduleServiceImpl.deleteSchedule, which throws AccessDeniedException (-> 403)
     * otherwise. Both roles share this one endpoint rather than a separate "/me/{id}" variant,
     * since the ownership check already fully covers the self-service case.
     */
    @RoleRequired({"MANAGER", "TRAINER"})
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSchedule(@PathVariable Integer id) {
        trainerScheduleService.deleteSchedule(id);
        return ResponseEntity.noContent().build();
    }
}
