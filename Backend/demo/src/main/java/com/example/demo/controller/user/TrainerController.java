package com.example.demo.controller.user;

import com.example.demo.annotation.RoleRequired;
import com.example.demo.dto.user.TrainerDTO;
import com.example.demo.service.user.TrainerService;
import com.example.demo.service.params.request.user.trainer.CreateTrainerRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @RoleRequired("MANAGER")
    @GetMapping("/{id}")
    public ResponseEntity<TrainerDTO> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(trainerService.getById(id));
    }

    @RoleRequired("MANAGER")
    @PutMapping("/{id}")
    public ResponseEntity<TrainerDTO> update(@PathVariable Integer id, @RequestBody CreateTrainerRequest request) {
        return ResponseEntity.ok(trainerService.update(id, request));
    }

    @RoleRequired("MANAGER")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        trainerService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
