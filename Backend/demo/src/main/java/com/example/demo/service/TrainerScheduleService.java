package com.example.demo.service;

import com.example.demo.dto.TrainerScheduleDTO;
import com.example.demo.service.params.request.Schedule.CreateTrainerScheduleRequest;
import com.example.demo.service.params.request.Schedule.CreateTrainerUnavailabilityRequest;

public interface TrainerScheduleService {
    TrainerScheduleDTO createSchedule(CreateTrainerScheduleRequest request);

    void createUnavailability(CreateTrainerUnavailabilityRequest request);
}
