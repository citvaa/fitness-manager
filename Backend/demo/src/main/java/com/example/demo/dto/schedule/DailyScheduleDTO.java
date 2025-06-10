package com.example.demo.dto.schedule;

import com.example.demo.dto.AppointmentDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class DailyScheduleDTO {
    private LocalDate date;
    private List<AppointmentDTO> appointments;
}
