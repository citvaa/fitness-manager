package com.example.demo.service.schedule;

import com.example.demo.dto.schedule.GymScheduleDTO;
import com.example.demo.service.params.request.schedule.CreateGymScheduleRequest;

import java.util.List;

public interface GymScheduleService {
    GymScheduleDTO create(CreateGymScheduleRequest request);

    List<GymScheduleDTO> getAll();
}
