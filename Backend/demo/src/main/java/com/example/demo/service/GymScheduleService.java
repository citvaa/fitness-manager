package com.example.demo.service;

import com.example.demo.dto.GymScheduleDTO;
import com.example.demo.service.params.request.Schedule.CreateGymScheduleRequest;

public interface GymScheduleService {
    GymScheduleDTO create(CreateGymScheduleRequest request);
}
