package com.example.demo.mapper;

import com.example.demo.dto.schedule.GymScheduleDTO;
import com.example.demo.model.schedule.GymSchedule;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface GymScheduleMapper {
    GymScheduleDTO toDto(GymSchedule gymSchedule);
}
