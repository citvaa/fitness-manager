package com.example.demo.service.impl;

import com.example.demo.dto.HolidayScheduleDTO;
import com.example.demo.mapper.HolidayScheduleMapper;
import com.example.demo.model.HolidaySchedule;
import com.example.demo.repository.HolidayScheduleRepository;
import com.example.demo.service.HolidayScheduleService;
import com.example.demo.service.params.request.Schedule.CreateHolidayScheduleRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@RequiredArgsConstructor
@Service
public class HolidayScheduleServiceImpl implements HolidayScheduleService {

    private final HolidayScheduleMapper holidayScheduleMapper;
    private final HolidayScheduleRepository holidayScheduleRepository;

    @Override
    public HolidayScheduleDTO create(CreateHolidayScheduleRequest request) {
        HolidaySchedule holidaySchedule = holidayScheduleMapper.toEntity(request);
        HolidaySchedule savedHolidaySchedule = holidayScheduleRepository.save(holidaySchedule);
        return holidayScheduleMapper.toDTO(savedHolidaySchedule);
    }

    @Override
    public boolean isGymClosedOn(LocalDate date) {
        return holidayScheduleRepository.existsByDate(date);
    }
}
