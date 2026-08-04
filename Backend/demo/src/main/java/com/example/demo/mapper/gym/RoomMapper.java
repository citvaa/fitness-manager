package com.example.demo.mapper.gym;

import com.example.demo.dto.gym.RoomDTO;
import com.example.demo.model.gym.Room;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = GymMapper.class)
public interface RoomMapper {
    RoomDTO toDto(Room room);
    Room toEntity(RoomDTO roomDTO);
}
