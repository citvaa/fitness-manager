package com.example.demo.mapper.gym;

import com.example.demo.dto.gym.RoomDTO;
import com.example.demo.dto.summary.RoomSummaryDTO;
import com.example.demo.model.gym.Room;
import org.mapstruct.Mapper;
import org.mapstruct.Named;

@Mapper(componentModel = "spring", uses = GymMapper.class)
public interface RoomMapper {
    RoomDTO toDto(Room room);

    @Named("toSummaryDto")
    RoomSummaryDTO toSummaryDto(Room room);

    Room toEntity(RoomDTO roomDTO);
}
