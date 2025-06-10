package com.example.demo.mapper;

import com.example.demo.dto.schedule.TrainerScheduleDTO;
import com.example.demo.model.schedule.TrainerSchedule;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TrainerScheduleMapper {
    TrainerScheduleDTO toDto(TrainerSchedule trainerSchedule);
}
