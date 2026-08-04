package com.example.demo.mapper.gym;

import com.example.demo.dto.gym.RoomDTO;
import com.example.demo.dto.summary.RoomSummaryDTO;
import com.example.demo.model.gym.Room;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "spring", uses = GymMapper.class)
public interface RoomMapper {

    @Mapping(target = "gym", source = "gym", qualifiedByName = "toSummaryDto")
    RoomDTO toDto(Room room);

    List<RoomDTO> toDto(List<Room> rooms);

    @Named("toSummaryDto")
    RoomSummaryDTO toSummaryDto(Room room);
}
