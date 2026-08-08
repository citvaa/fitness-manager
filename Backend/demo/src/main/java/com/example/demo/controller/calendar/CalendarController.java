package com.example.demo.controller.calendar;

import com.example.demo.annotation.RoleRequired;
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

    /**
     * Fixed a real authorization gap (see AGENTS.md "Known issues"): this endpoint had no
     * @RoleRequired at all, so any authenticated user of any role - including CLIENT - could
     * pull the full gym-wide daily schedule (every appointment, every client/trainer on it).
     * Scoped to MANAGER and TRAINER, who both have a legitimate operational reason to see the
     * whole day's schedule (a trainer checking room/time overlaps isn't limited to their own
     * appointments elsewhere in the codebase either); CLIENT is excluded since this is
     * gym-wide, not "my own appointments" - see AGENTS.md "Upgrade: Faza 6 decisions" (continued).
     */
    @RoleRequired({"MANAGER", "TRAINER"})
    @Operation(summary = "Get schedule for a specific day",
            description = "Retrieves all appointments for the given date. Format example: `YYYY-MM-DD`.")
    @GetMapping
    public ResponseEntity<DailyScheduleDTO> getScheduleForDay(@RequestParam LocalDate date) {
        DailyScheduleDTO schedule = calendarService.getDailySchedule(date);
        return ResponseEntity.ok(schedule);
    }
}
