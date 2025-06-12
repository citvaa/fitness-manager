package com.example.demo.service.schedule;

import com.example.demo.dto.schedule.DailyScheduleDTO;

import java.time.LocalDate;

public interface CalendarService {
    DailyScheduleDTO getDailySchedule(LocalDate date);
}
