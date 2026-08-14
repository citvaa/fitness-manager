package com.example.demo.service.params.request.appointment;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class CreateAppointmentRequest {
    private LocalDate date;

    @Schema(example = "00:00:00")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm:ss")
    private LocalTime startTime;

    @Schema(example = "00:00:00")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm:ss")
    private LocalTime endTime;

    private Integer sessionId;
    private Integer trainerId = null;
    private Integer roomId = null;
    private Set<Integer> clientIds;

    /** When true, {@code date} is treated as the first occurrence of a weekly-recurring slot
     * (e.g. "every Wednesday 18-19h") - see AppointmentService#createRecurringWeekly and
     * AGENTS.md "Upgrade: fixed weekly appointment decisions" for how many instances get
     * generated. Ignored by the plain single-appointment create(). */
    private boolean recurring = false;
}
