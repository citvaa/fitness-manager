package com.example.demo.service.schedule;

import com.example.demo.dto.schedule.TrainerScheduleDTO;
import com.example.demo.service.params.request.schedule.CreateOwnTrainerScheduleRequest;
import com.example.demo.service.params.request.schedule.CreateOwnTrainerUnavailabilityRequest;

import java.util.List;

public interface TrainerScheduleService {
    /** MANAGER-facing - any trainer's full schedule, oldest first. */
    List<TrainerScheduleDTO> getSchedule(Integer trainerId);

    /** TRAINER-facing self-service - the logged-in trainer's own schedule (resolved from the JWT). */
    List<TrainerScheduleDTO> getMySchedule();

    /** TRAINER-facing self-service - create a schedule entry for the logged-in trainer only. */
    TrainerScheduleDTO createMySchedule(CreateOwnTrainerScheduleRequest request);

    /** TRAINER-facing self-service - "fiksni raspored": generates weekly-repeating WORKING shifts
     * for the logged-in trainer, same convention as AppointmentService#createRecurringWeekly (8
     * weeks ahead from {@code request.getDate()}, one week's conflict skipped rather than fatal
     * to the whole series). See AGENTS.md "Upgrade: trainer fixed-schedule decisions". */
    List<TrainerScheduleDTO> createMyScheduleRecurring(CreateOwnTrainerScheduleRequest request);

    /** TRAINER-facing self-service - create an unavailability period for the logged-in trainer only. */
    void createMyUnavailability(CreateOwnTrainerUnavailabilityRequest request);

    /**
     * Delete a schedule entry. A TRAINER may only delete their own (enforced against the
     * JWT-derived trainer id) - throws AccessDeniedException otherwise. MANAGER no longer has a
     * write bypass here - see AGENTS.md "Upgrade: manager schedule-write removal decisions".
     */
    void deleteSchedule(Integer id);
}
