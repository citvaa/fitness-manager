package com.example.demo.service.gym;

import com.example.demo.dto.gym.RoomCheckInDTO;
import com.example.demo.dto.gym.RoomOccupancyDTO;

import java.util.List;

public interface RoomCheckInService {

    RoomCheckInDTO checkIn(Integer roomId, Integer clientId);

    RoomCheckInDTO checkOut(Integer checkInId);

    RoomOccupancyDTO getOccupancy(Integer roomId);

    List<RoomOccupancyDTO> getAllOccupancy();
}
