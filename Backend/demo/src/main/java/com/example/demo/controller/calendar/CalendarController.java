package com.example.demo.controller.calendar;

import com.example.demo.dto.schedule.DailyScheduleDTO;
import com.example.demo.service.schedule.CalendarService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/calendar")
public class CalendarController {

    private final CalendarService calendarService;

    @Operation(summary = "Get schedule for a specific day",
            description = "Retrieves all appointments for the given date. Format example: `YYYY-MM-DD`.")
    @GetMapping
    public ResponseEntity<DailyScheduleDTO> getScheduleForDay(@RequestParam LocalDate date) {
        DailyScheduleDTO schedule = calendarService.getDailySchedule(date);
        return ResponseEntity.ok(schedule);
    }
}
