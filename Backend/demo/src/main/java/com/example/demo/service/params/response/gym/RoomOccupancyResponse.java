package com.example.demo.service.params.response.gym;

public record RoomOccupancyResponse(
        Integer roomId,
        String roomName,
        Integer capacity,
        long manualCheckIns,
        long scheduledParticipants,
        long totalOccupancy
) { }
