package com.example.demo.controller.schedule;

import com.example.demo.annotation.RoleRequired;
import com.example.demo.dto.schedule.GymScheduleDTO;
import com.example.demo.service.schedule.GymScheduleService;
import com.example.demo.service.params.request.schedule.CreateGymScheduleRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/schedule/gym")
public class GymScheduleController {

    private final GymScheduleService gymScheduleService;

    @RoleRequired("MANAGER")
    @GetMapping
    public List<GymScheduleDTO> getAll() { return gymScheduleService.getAll(); }

    @RoleRequired("MANAGER")
    @PostMapping
    public ResponseEntity<GymScheduleDTO> create(@RequestBody CreateGymScheduleRequest request) {
        GymScheduleDTO scheduleDTO = gymScheduleService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(scheduleDTO);
    }

    @RoleRequired("MANAGER") @PutMapping("/{id}")
    public GymScheduleDTO update(@PathVariable Integer id, @RequestBody CreateGymScheduleRequest request) { return gymScheduleService.update(id, request); }

    @RoleRequired("MANAGER") @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) { gymScheduleService.delete(id); return ResponseEntity.noContent().build(); }
}
