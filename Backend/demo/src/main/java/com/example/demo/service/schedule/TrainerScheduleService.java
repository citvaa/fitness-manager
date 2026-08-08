package com.example.demo.service.schedule;

import com.example.demo.dto.schedule.TrainerScheduleDTO;
import com.example.demo.service.params.request.schedule.CreateOwnTrainerScheduleRequest;
import com.example.demo.service.params.request.schedule.CreateOwnTrainerUnavailabilityRequest;
import com.example.demo.service.params.request.schedule.CreateTrainerScheduleRequest;
import com.example.demo.service.params.request.schedule.CreateTrainerUnavailabilityRequest;

import java.util.List;

public interface TrainerScheduleService {
    TrainerScheduleDTO createSchedule(CreateTrainerScheduleRequest request);

    void createUnavailability(CreateTrainerUnavailabilityRequest request);

    /** MANAGER-facing - any trainer's full schedule, oldest first. */
    List<TrainerScheduleDTO> getSchedule(Integer trainerId);

    /** TRAINER-facing self-service - the logged-in trainer's own schedule (resolved from the JWT). */
    List<TrainerScheduleDTO> getMySchedule();

    /** TRAINER-facing self-service - create a schedule entry for the logged-in trainer only. */
    TrainerScheduleDTO createMySchedule(CreateOwnTrainerScheduleRequest request);

    /** TRAINER-facing self-service - create an unavailability period for the logged-in trainer only. */
    void createMyUnavailability(CreateOwnTrainerUnavailabilityRequest request);

    /**
     * Delete a schedule entry. MANAGER may delete any trainer's entry; a TRAINER may only delete
     * their own (enforced against the JWT-derived trainer id) - throws AccessDeniedException
     * otherwise. See AGENTS.md "Upgrade: Faza 6 decisions".
     */
    void deleteSchedule(Integer id);
}
