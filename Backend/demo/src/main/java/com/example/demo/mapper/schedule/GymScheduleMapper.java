package com.example.demo.mapper.schedule;

import com.example.demo.dto.schedule.GymScheduleDTO;
import com.example.demo.model.schedule.GymSchedule;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface GymScheduleMapper {
    GymScheduleDTO toDto(GymSchedule gymSchedule);

    List<GymScheduleDTO> toDto(List<GymSchedule> gymSchedules);
}
