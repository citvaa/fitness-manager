package com.example.demo.dto;

import com.example.demo.dto.summary.ClientSummaryDTO;
import com.example.demo.dto.summary.TrainerSummaryDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class AppointmentDTO {
    private Integer id;
    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;
    private SessionDTO session;
    private TrainerSummaryDTO trainer;
    private Set<ClientSummaryDTO> clients;
}
