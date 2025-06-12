package com.example.demo.controller;

import com.example.demo.annotation.RoleRequired;
import com.example.demo.dto.schedule.TrainerScheduleDTO;
import com.example.demo.service.schedule.TrainerScheduleService;
import com.example.demo.service.params.request.schedule.CreateTrainerScheduleRequest;
import com.example.demo.service.params.request.schedule.CreateTrainerUnavailabilityRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
