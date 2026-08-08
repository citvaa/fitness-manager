package com.example.demo.mapper.schedule;

import com.example.demo.dto.schedule.TrainerScheduleDTO;
import com.example.demo.model.schedule.TrainerSchedule;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface TrainerScheduleMapper {
    TrainerScheduleDTO toDto(TrainerSchedule trainerSchedule);

    List<TrainerScheduleDTO> toDto(List<TrainerSchedule> trainerSchedules);
}
