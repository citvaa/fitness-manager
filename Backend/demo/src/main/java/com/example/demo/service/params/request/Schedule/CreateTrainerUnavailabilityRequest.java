package com.example.demo.service.params.request.Schedule;

import com.example.demo.enums.WorkStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class CreateTrainerUnavailabilityRequest {
    private Integer trainerId;
    private LocalDate startDate;
    private LocalDate endDate;
    private WorkStatus status;
}
