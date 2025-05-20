package com.example.demo.mapper;

import com.example.demo.dto.HolidayScheduleDTO;
import com.example.demo.model.HolidaySchedule;
import com.example.demo.service.params.request.Schedule.CreateHolidayScheduleRequest;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface HolidayScheduleMapper {
    HolidaySchedule toEntity(CreateHolidayScheduleRequest request);
    HolidayScheduleDTO toDTO(HolidaySchedule entity);
}
