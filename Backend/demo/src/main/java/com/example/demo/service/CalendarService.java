package com.example.demo.service;

import com.example.demo.dto.DailyScheduleDTO;

import java.time.LocalDate;

public interface CalendarService {
    DailyScheduleDTO getDailySchedule(LocalDate date);
}
