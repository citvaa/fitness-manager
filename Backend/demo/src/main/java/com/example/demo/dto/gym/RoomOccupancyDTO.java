package com.example.demo.dto.gym;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Computed occupancy signal for a room - combines currently-active manual check-ins
 * ({@code RoomCheckIn} rows with a null {@code checkedOutAt}) with clients on in-progress
 * appointments in that room. This is the payload sent both as an HTTP response and as the
 * WebSocket message on {@code /topic/gym/occupancy} - see AGENTS.md ("Upgrade: service layer
 * decisions") for the message-format rationale.
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class RoomOccupancyDTO {
    private Integer roomId;
    private String roomName;
    private Integer capacity;
    private Integer checkedInCount;
    private Integer appointmentOccupantCount;
    private Integer totalOccupancy;
    private Double occupancyPercent;
    private boolean atCapacity;
}
