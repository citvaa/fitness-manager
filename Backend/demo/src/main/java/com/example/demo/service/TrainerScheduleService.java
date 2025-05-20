package com.example.demo.service;

import com.example.demo.dto.TrainerScheduleDTO;
import com.example.demo.service.params.request.Schedule.CreateTrainerScheduleRequest;

public interface TrainerScheduleService {
    TrainerScheduleDTO create(CreateTrainerScheduleRequest request);
}
