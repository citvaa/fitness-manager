package com.example.demo.service.gym;

import com.example.demo.dto.gym.GymDTO;
import com.example.demo.dto.gym.RoomDTO;
import com.example.demo.service.params.request.gym.UpsertGymRequest;
import com.example.demo.service.params.request.gym.UpsertRoomRequest;
import java.util.List;

public interface GymFloorPlanService {
    GymDTO getGym();
    GymDTO createGym(UpsertGymRequest request);
    GymDTO updateGym(Integer id, UpsertGymRequest request);
    void deleteGym(Integer id);
    List<RoomDTO> getRooms();
    RoomDTO getRoom(Integer id);
    RoomDTO createRoom(UpsertRoomRequest request);
    RoomDTO updateRoom(Integer id, UpsertRoomRequest request);
    void deleteRoom(Integer id);
}
