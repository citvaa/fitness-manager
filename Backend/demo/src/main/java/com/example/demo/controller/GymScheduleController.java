package com.example.demo.controller;

import com.example.demo.annotation.RoleRequired;
import com.example.demo.dto.GymScheduleDTO;
import com.example.demo.service.GymScheduleService;
import com.example.demo.service.params.request.Schedule.CreateScheduleRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/schedule/gym")
public class GymScheduleController {

    private final GymScheduleService gymScheduleService;

    @RoleRequired("MANAGER")
    @PostMapping
    public ResponseEntity<GymScheduleDTO> create(@RequestBody CreateScheduleRequest request) {
        GymScheduleDTO scheduleDTO = gymScheduleService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(scheduleDTO);
    }
}
