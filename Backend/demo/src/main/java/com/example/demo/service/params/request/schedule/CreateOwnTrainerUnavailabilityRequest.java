package com.example.demo.service.params.request.schedule;

import com.example.demo.enums.WorkStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Same shape as {@link CreateTrainerUnavailabilityRequest} minus {@code trainerId} - see
 * {@link CreateOwnTrainerScheduleRequest} for the rationale.
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class CreateOwnTrainerUnavailabilityRequest {
    private LocalDate startDate;
    private LocalDate endDate;
    private WorkStatus status;
}
