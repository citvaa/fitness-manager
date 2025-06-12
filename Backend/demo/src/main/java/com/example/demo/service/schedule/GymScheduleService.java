package com.example.demo.service.schedule;

import com.example.demo.dto.schedule.GymScheduleDTO;
import com.example.demo.service.params.request.schedule.CreateGymScheduleRequest;

public interface GymScheduleService {
    GymScheduleDTO create(CreateGymScheduleRequest request);
}
