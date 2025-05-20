package com.example.demo.controller;

import com.example.demo.annotation.RoleRequired;
import com.example.demo.dto.TrainerScheduleDTO;
import com.example.demo.service.TrainerScheduleService;
import com.example.demo.service.params.request.Schedule.CreateTrainerScheduleRequest;
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
    public ResponseEntity<TrainerScheduleDTO> createTrainerSchedule(@RequestBody CreateTrainerScheduleRequest request) {
        TrainerScheduleDTO trainerScheduleDTO = trainerScheduleService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(trainerScheduleDTO);
    }
}
