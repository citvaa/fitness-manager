package com.example.demo.controller.schedule;

import com.example.demo.annotation.RoleRequired;
import com.example.demo.dto.schedule.TrainerScheduleDTO;
import com.example.demo.service.schedule.TrainerScheduleService;
import com.example.demo.service.params.request.schedule.CreateTrainerScheduleRequest;
import com.example.demo.service.params.request.schedule.CreateTrainerUnavailabilityRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/schedule/trainer")
public class TrainerScheduleController {

    private final TrainerScheduleService trainerScheduleService;

    @RoleRequired("MANAGER") @GetMapping
    public List<TrainerScheduleDTO> getByTrainer(@RequestParam Integer trainerId) { return trainerScheduleService.getByTrainer(trainerId); }

    @RoleRequired("TRAINER") @GetMapping("/me")
    public List<TrainerScheduleDTO> getOwn() { return trainerScheduleService.getOwn(); }

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

    @RoleRequired("MANAGER") @PutMapping("/{id}")
    public TrainerScheduleDTO update(@PathVariable Integer id, @RequestBody CreateTrainerScheduleRequest request) { return trainerScheduleService.update(id, request, false); }
    @RoleRequired("MANAGER") @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) { trainerScheduleService.delete(id, false); return ResponseEntity.noContent().build(); }

    @RoleRequired("TRAINER") @PostMapping("/me")
    public ResponseEntity<TrainerScheduleDTO> createOwn(@RequestBody CreateTrainerScheduleRequest request) { return ResponseEntity.status(HttpStatus.CREATED).body(trainerScheduleService.createOwnSchedule(request)); }
    @RoleRequired("TRAINER") @PostMapping("/me/unavailable")
    public ResponseEntity<Void> createOwnUnavailable(@RequestBody CreateTrainerUnavailabilityRequest request) { trainerScheduleService.createOwnUnavailability(request); return ResponseEntity.status(HttpStatus.CREATED).build(); }
    @RoleRequired("TRAINER") @PutMapping("/me/{id}")
    public TrainerScheduleDTO updateOwn(@PathVariable Integer id, @RequestBody CreateTrainerScheduleRequest request) { return trainerScheduleService.update(id, request, true); }
    @RoleRequired("TRAINER") @DeleteMapping("/me/{id}")
    public ResponseEntity<Void> deleteOwn(@PathVariable Integer id) { trainerScheduleService.delete(id, true); return ResponseEntity.noContent().build(); }
}
