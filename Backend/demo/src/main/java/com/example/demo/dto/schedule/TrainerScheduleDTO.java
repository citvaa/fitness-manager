package com.example.demo.dto.schedule;

import com.example.demo.dto.user.TrainerDTO;
import com.example.demo.enums.WorkStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class TrainerScheduleDTO {
    private Integer id;
    private TrainerDTO trainer;
    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;
    private WorkStatus status;
}
