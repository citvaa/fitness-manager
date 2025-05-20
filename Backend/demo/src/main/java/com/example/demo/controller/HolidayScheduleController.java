package com.example.demo.controller;

import com.example.demo.annotation.RoleRequired;
import com.example.demo.dto.HolidayScheduleDTO;
import com.example.demo.service.HolidayScheduleService;
import com.example.demo.service.params.request.Schedule.CreateHolidayScheduleRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/schedule/holiday")
public class HolidayScheduleController {

    private final HolidayScheduleService holidayScheduleService;

    @RoleRequired("MANAGER")
    @PostMapping
    public ResponseEntity<HolidayScheduleDTO> create(@RequestBody CreateHolidayScheduleRequest request) {
        HolidayScheduleDTO holidayScheduleDTO = holidayScheduleService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(holidayScheduleDTO);
    }
}
