package com.example.demo.service.impl;

import com.example.demo.dto.HolidayDTO;
import com.example.demo.mapper.HolidayMapper;
import com.example.demo.model.Holiday;
import com.example.demo.repository.HolidayRepository;
import com.example.demo.service.HolidayService;
import com.example.demo.service.params.request.schedule.CreateHolidayRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Transactional(readOnly = true)
@RequiredArgsConstructor
@Service
public class HolidayServiceImpl implements HolidayService {

    private final HolidayMapper holidayMapper;
    private final HolidayRepository holidayRepository;

    @Transactional
    public HolidayDTO create(CreateHolidayRequest request) {
        Holiday holiday = holidayMapper.toEntity(request);
        Holiday savedHoliday = holidayRepository.save(holiday);
        return holidayMapper.toDTO(savedHoliday);
    }

    public boolean isGymClosedOn(LocalDate date) {
        return holidayRepository.existsByDate(date);
    }
}
