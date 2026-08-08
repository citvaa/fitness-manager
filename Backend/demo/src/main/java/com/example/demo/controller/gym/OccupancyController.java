package com.example.demo.controller.gym;

import com.example.demo.annotation.RoleRequired;
import com.example.demo.dto.gym.RoomCheckInDTO;
import com.example.demo.dto.user.ClientDTO;
import com.example.demo.service.gym.OccupancyService;
import com.example.demo.service.user.ClientService;
import com.example.demo.service.params.request.gym.RoomCheckInRequest;
import com.example.demo.service.params.response.gym.OccupancySnapshotResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/gym/occupancy")
public class OccupancyController {
    private final OccupancyService service;
    private final ClientService clientService;

    @RoleRequired({"MANAGER", "TRAINER"})
    @GetMapping("/clients")
    public java.util.List<ClientDTO> clients() { return clientService.getAll(); }

    @RoleRequired({"MANAGER", "TRAINER"})
    @PostMapping("/check-ins")
    public ResponseEntity<RoomCheckInDTO> checkIn(@RequestBody RoomCheckInRequest request) { return ResponseEntity.status(HttpStatus.CREATED).body(service.checkIn(request)); }

    @RoleRequired({"MANAGER", "TRAINER"})
    @PostMapping("/check-outs/{clientId}")
    public RoomCheckInDTO checkOut(@PathVariable Integer clientId) { return service.checkOut(clientId); }

    @RoleRequired({"MANAGER", "TRAINER", "CLIENT"})
    @GetMapping
    public OccupancySnapshotResponse current() { return service.currentOccupancy(); }
}
