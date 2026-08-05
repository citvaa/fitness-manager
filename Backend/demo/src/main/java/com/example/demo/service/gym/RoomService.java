package com.example.demo.service.gym;

import com.example.demo.dto.gym.RoomDTO;
import com.example.demo.service.params.request.gym.CreateRoomRequest;
import com.example.demo.service.params.request.gym.UpdateRoomRequest;

import java.util.List;

public interface RoomService {

    RoomDTO create(CreateRoomRequest request);

    RoomDTO update(Integer id, UpdateRoomRequest request);

    void delete(Integer id);

    RoomDTO getById(Integer id);

    List<RoomDTO> getAll();
}
