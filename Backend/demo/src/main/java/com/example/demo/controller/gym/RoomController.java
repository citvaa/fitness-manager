package com.example.demo.controller.gym;

import com.example.demo.annotation.RoleRequired;
import com.example.demo.dto.gym.RoomDTO;
import com.example.demo.service.gym.RoomService;
import com.example.demo.service.params.request.gym.CreateRoomRequest;
import com.example.demo.service.params.request.gym.UpdateRoomRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Floor-plan rooms. Only MANAGER edits the floor plan (create/update/delete); reads are open to
 * any authenticated user - trainers/clients need room names for check-in and the live floor plan
 * view. See AGENTS.md ("Upgrade: service layer decisions").
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/gym/room")
public class RoomController {

    private final RoomService roomService;

    @RoleRequired("MANAGER")
    @PostMapping
    public ResponseEntity<RoomDTO> create(@RequestBody CreateRoomRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(roomService.create(request));
    }

    @RoleRequired("MANAGER")
    @PutMapping("/{id}")
    public ResponseEntity<RoomDTO> update(@PathVariable Integer id, @RequestBody UpdateRoomRequest request) {
        return ResponseEntity.ok(roomService.update(id, request));
    }

    @RoleRequired("MANAGER")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        roomService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @RoleRequired({"MANAGER", "TRAINER", "CLIENT"})
    @GetMapping("/{id}")
    public ResponseEntity<RoomDTO> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(roomService.getById(id));
    }

    @RoleRequired({"MANAGER", "TRAINER", "CLIENT"})
    @GetMapping
    public ResponseEntity<List<RoomDTO>> getAll() {
        return ResponseEntity.ok(roomService.getAll());
    }
}
