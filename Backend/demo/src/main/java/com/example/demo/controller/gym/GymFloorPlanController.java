package com.example.demo.controller.gym;

import com.example.demo.annotation.RoleRequired;
import com.example.demo.dto.gym.GymDTO;
import com.example.demo.dto.gym.RoomDTO;
import com.example.demo.service.gym.GymFloorPlanService;
import com.example.demo.service.params.request.gym.UpsertGymRequest;
import com.example.demo.service.params.request.gym.UpsertRoomRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/gym")
public class GymFloorPlanController {
    private final GymFloorPlanService service;

    @RoleRequired({"MANAGER", "TRAINER", "CLIENT"})
    @GetMapping
    public GymDTO getGym() { return service.getGym(); }

    @RoleRequired("MANAGER")
    @PostMapping
    public ResponseEntity<GymDTO> createGym(@RequestBody UpsertGymRequest request) { return ResponseEntity.status(HttpStatus.CREATED).body(service.createGym(request)); }

    @RoleRequired("MANAGER")
    @PutMapping("/{id}")
    public GymDTO updateGym(@PathVariable Integer id, @RequestBody UpsertGymRequest request) { return service.updateGym(id, request); }

    @RoleRequired("MANAGER")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGym(@PathVariable Integer id) { service.deleteGym(id); return ResponseEntity.noContent().build(); }

    @RoleRequired({"MANAGER", "TRAINER", "CLIENT"})
    @GetMapping("/rooms")
    public List<RoomDTO> getRooms() { return service.getRooms(); }

    @RoleRequired({"MANAGER", "TRAINER", "CLIENT"})
    @GetMapping("/rooms/{id}")
    public RoomDTO getRoom(@PathVariable Integer id) { return service.getRoom(id); }

    @RoleRequired("MANAGER")
    @PostMapping("/rooms")
    public ResponseEntity<RoomDTO> createRoom(@RequestBody UpsertRoomRequest request) { return ResponseEntity.status(HttpStatus.CREATED).body(service.createRoom(request)); }

    @RoleRequired("MANAGER")
    @PutMapping("/rooms/{id}")
    public RoomDTO updateRoom(@PathVariable Integer id, @RequestBody UpsertRoomRequest request) { return service.updateRoom(id, request); }

    @RoleRequired("MANAGER")
    @DeleteMapping("/rooms/{id}")
    public ResponseEntity<Void> deleteRoom(@PathVariable Integer id) { service.deleteRoom(id); return ResponseEntity.noContent().build(); }
}
