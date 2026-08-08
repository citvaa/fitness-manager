package com.example.demo.service.params.request.schedule;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Same shape as {@link CreateTrainerScheduleRequest} minus {@code trainerId} - the self-service
 * trainer endpoints (POST /api/schedule/trainer/me) derive the trainer from the caller's JWT
 * instead of trusting a client-supplied id, so a trainer can never write another trainer's
 * schedule by tampering with the request body. See AGENTS.md "Upgrade: Faza 6 decisions".
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class CreateOwnTrainerScheduleRequest {
    private LocalDate date;

    @Schema(example = "00:00:00")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm:ss")
    private LocalTime startTime;

    @Schema(example = "00:00:00")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm:ss")
    private LocalTime endTime;
}
