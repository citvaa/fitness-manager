package com.example.demo.controller;

import com.example.demo.annotation.RoleRequired;
import com.example.demo.dto.TrainerDTO;
import com.example.demo.service.TrainerService;
import com.example.demo.service.params.request.Trainer.CreateTrainerRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/trainer")
public class TrainerController {

    private final TrainerService trainerService;

    @RoleRequired("MANAGER")
    @PostMapping
    public ResponseEntity<TrainerDTO> create(@RequestBody CreateTrainerRequest request) {
        TrainerDTO createdTrainer = trainerService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdTrainer);
    }
}
