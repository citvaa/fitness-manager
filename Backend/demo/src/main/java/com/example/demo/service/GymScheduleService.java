package com.example.demo.service;

import com.example.demo.dto.GymScheduleDTO;
import com.example.demo.service.params.request.Schedule.CreateScheduleRequest;

public interface GymScheduleService {
    GymScheduleDTO create(CreateScheduleRequest request);
}
