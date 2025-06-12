package com.example.demo.service.schedule;

import com.example.demo.dto.schedule.TrainerScheduleDTO;
import com.example.demo.service.params.request.schedule.CreateTrainerScheduleRequest;
import com.example.demo.service.params.request.schedule.CreateTrainerUnavailabilityRequest;

public interface TrainerScheduleService {
    TrainerScheduleDTO createSchedule(CreateTrainerScheduleRequest request);

    void createUnavailability(CreateTrainerUnavailabilityRequest request);
}
