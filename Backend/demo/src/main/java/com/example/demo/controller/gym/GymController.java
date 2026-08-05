package com.example.demo.controller.gym;

import com.example.demo.annotation.RoleRequired;
import com.example.demo.dto.gym.GymDTO;
import com.example.demo.service.gym.GymService;
import com.example.demo.service.params.request.gym.UpsertGymRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Gym installation config. Read is open to any authenticated user (clients/trainers need it for
 * branding/timezone display); only MANAGER can edit it - see AGENTS.md ("Upgrade: service layer
 * decisions").
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/gym")
public class GymController {

    private final GymService gymService;

    @RoleRequired({"MANAGER", "TRAINER", "CLIENT"})
    @GetMapping
    public ResponseEntity<GymDTO> getGym() {
        return ResponseEntity.ok(gymService.getGym());
    }

    @RoleRequired("MANAGER")
    @PutMapping
    public ResponseEntity<GymDTO> upsertGym(@RequestBody UpsertGymRequest request) {
        return ResponseEntity.ok(gymService.upsertGym(request));
    }
}
