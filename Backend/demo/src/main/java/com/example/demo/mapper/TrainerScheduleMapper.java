package com.example.demo.mapper;

import com.example.demo.dto.TrainerScheduleDTO;
import com.example.demo.model.TrainerSchedule;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TrainerScheduleMapper {
    TrainerScheduleDTO toDto(TrainerSchedule trainerSchedule);
}
