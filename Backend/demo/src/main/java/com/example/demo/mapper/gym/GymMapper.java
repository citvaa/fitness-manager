package com.example.demo.mapper.gym;

import com.example.demo.dto.gym.GymDTO;
import com.example.demo.dto.summary.GymSummaryDTO;
import com.example.demo.model.gym.Gym;
import org.mapstruct.Mapper;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "spring")
public interface GymMapper {

    GymDTO toDto(Gym gym);

    List<GymDTO> toDto(List<Gym> gyms);

    @Named("toSummaryDto")
    GymSummaryDTO toSummaryDto(Gym gym);
}
