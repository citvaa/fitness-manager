package com.example.demo.service;

import com.example.demo.dto.HolidayScheduleDTO;
import com.example.demo.service.params.request.Schedule.CreateHolidayScheduleRequest;

import java.time.LocalDate;

public interface HolidayScheduleService {
    HolidayScheduleDTO create(CreateHolidayScheduleRequest request);

    boolean isGymClosedOn(LocalDate date);
}
