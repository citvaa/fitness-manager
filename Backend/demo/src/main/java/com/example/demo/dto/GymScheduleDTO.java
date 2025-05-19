package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.DayOfWeek;
import java.time.LocalTime;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class GymScheduleDTO {
    private Integer id;
    private DayOfWeek day;
    private LocalTime openingTime;
    private LocalTime closingTime;
}
