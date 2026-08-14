package com.example.demo.service.gym;

import com.example.demo.dto.gym.RoomCheckInDTO;
import com.example.demo.dto.gym.RoomOccupancyDTO;

import java.util.List;
import java.util.Optional;

public interface RoomCheckInService {

    RoomCheckInDTO checkIn(Integer roomId, Integer clientId);

    RoomCheckInDTO checkOut(Integer checkInId);

    RoomOccupancyDTO getOccupancy(Integer roomId);

    List<RoomOccupancyDTO> getAllOccupancy();

    /** A client's currently-open check-in (any room), if any - backs the TRAINER "Započni
     * trening" client check-in panel's Check-in/Check-out toggle per client. See AGENTS.md
     * "Upgrade: trainer check-in decisions". */
    Optional<RoomCheckInDTO> getActiveCheckInForClient(Integer clientId);
}
