package com.example.demo.service.gym;

import com.example.demo.dto.gym.RoomCheckInDTO;
import com.example.demo.service.params.request.gym.RoomCheckInRequest;
import com.example.demo.service.params.response.gym.OccupancySnapshotResponse;

public interface OccupancyService {
    RoomCheckInDTO checkIn(RoomCheckInRequest request);
    RoomCheckInDTO checkOut(Integer clientId);
    OccupancySnapshotResponse currentOccupancy();
    OccupancySnapshotResponse publishCurrentOccupancy();
}
