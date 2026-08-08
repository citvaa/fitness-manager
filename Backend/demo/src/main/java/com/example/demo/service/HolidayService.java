package com.example.demo.service;

import com.example.demo.dto.HolidayDTO;
import com.example.demo.service.params.request.schedule.CreateHolidayRequest;

import java.time.LocalDate;

public interface HolidayService {
    HolidayDTO create(CreateHolidayRequest request);

    boolean isGymClosedOn(LocalDate date);
    java.util.List<HolidayDTO> getAll();
    void delete(Integer id);
}
