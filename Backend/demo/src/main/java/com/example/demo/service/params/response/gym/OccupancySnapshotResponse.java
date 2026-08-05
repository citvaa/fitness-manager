package com.example.demo.service.params.response.gym;

import java.time.LocalDateTime;
import java.util.List;

public record OccupancySnapshotResponse(LocalDateTime generatedAt, List<RoomOccupancyResponse> rooms) { }
