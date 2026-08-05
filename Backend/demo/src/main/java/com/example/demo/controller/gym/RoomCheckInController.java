package com.example.demo.controller.gym;

import com.example.demo.annotation.RoleRequired;
import com.example.demo.dto.gym.RoomCheckInDTO;
import com.example.demo.dto.gym.RoomOccupancyDTO;
import com.example.demo.service.gym.RoomCheckInService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Manual room check-in/check-out is a front-desk/staff operation performed by MANAGER or TRAINER
 * on behalf of a client (not client self-service) - see AGENTS.md ("Upgrade: service layer
 * decisions"). Occupancy reads are open to any authenticated user for the live floor-plan view.
 * <p>
 * check-in/check-out catch {@link IllegalStateException} locally and return 409 with the message
 * - there is no global exception handler in this codebase (see AGENTS.md "Known issues"), so
 * without this the "one active check-in per client" violation would fall through to a bare 500
 * with no message, which fails the "return a meaningful error" requirement for this feature.
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/gym")
public class RoomCheckInController {

    private final RoomCheckInService roomCheckInService;

    @RoleRequired({"MANAGER", "TRAINER"})
    @PostMapping("/room/{roomId}/check-in")
    public ResponseEntity<?> checkIn(@PathVariable Integer roomId, @RequestParam Integer clientId) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(roomCheckInService.checkIn(roomId, clientId));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", e.getMessage()));
        }
    }

    @RoleRequired({"MANAGER", "TRAINER"})
    @PostMapping("/check-in/{checkInId}/check-out")
    public ResponseEntity<?> checkOut(@PathVariable Integer checkInId) {
        try {
            return ResponseEntity.ok(roomCheckInService.checkOut(checkInId));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", e.getMessage()));
        }
    }

    @RoleRequired({"MANAGER", "TRAINER", "CLIENT"})
    @GetMapping("/room/{roomId}/occupancy")
    public ResponseEntity<RoomOccupancyDTO> getOccupancy(@PathVariable Integer roomId) {
        return ResponseEntity.ok(roomCheckInService.getOccupancy(roomId));
    }

    @RoleRequired({"MANAGER", "TRAINER", "CLIENT"})
    @GetMapping("/occupancy")
    public ResponseEntity<List<RoomOccupancyDTO>> getAllOccupancy() {
        return ResponseEntity.ok(roomCheckInService.getAllOccupancy());
    }
}
