package com.example.demo.mapper.gym;

import com.example.demo.dto.gym.RoomCheckInDTO;
import com.example.demo.mapper.user.ClientMapper;
import com.example.demo.model.gym.RoomCheckIn;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring", uses = {RoomMapper.class, ClientMapper.class})
public interface RoomCheckInMapper {

    @Mapping(target = "room", source = "room", qualifiedByName = "toSummaryDto")
    @Mapping(target = "client", source = "client", qualifiedByName = "toSummaryDto")
    RoomCheckInDTO toDto(RoomCheckIn roomCheckIn);

    List<RoomCheckInDTO> toDto(List<RoomCheckIn> roomCheckIns);
}
