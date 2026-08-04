package com.example.demo.mapper.gym;

import com.example.demo.dto.gym.GymDTO;
import com.example.demo.model.gym.Gym;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface GymMapper {
    GymDTO toDto(Gym gym);
    Gym toEntity(GymDTO gymDTO);
}
