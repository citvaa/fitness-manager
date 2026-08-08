package com.example.demo.controller.calendar;

import com.example.demo.annotation.RoleRequired;
import com.example.demo.dto.HolidayDTO;
import com.example.demo.service.HolidayService;
import com.example.demo.service.params.request.schedule.CreateHolidayRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/schedule/holiday")
public class HolidayController {

    private final HolidayService holidayService;

    @RoleRequired("MANAGER")
    @PostMapping
    public ResponseEntity<HolidayDTO> create(@RequestBody CreateHolidayRequest request) {
        HolidayDTO holidayDTO = holidayService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(holidayDTO);
    }

    /** Open to any authenticated role - same reasoning as GymScheduleController.getAll(). */
    @GetMapping
    public ResponseEntity<List<HolidayDTO>> getAll() {
        return ResponseEntity.ok(holidayService.getAll());
    }
}
