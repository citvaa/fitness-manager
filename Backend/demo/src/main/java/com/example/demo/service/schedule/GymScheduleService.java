package com.example.demo.service.schedule;

import com.example.demo.dto.schedule.GymScheduleDTO;
import com.example.demo.service.params.request.schedule.CreateGymScheduleRequest;

public interface GymScheduleService {
    GymScheduleDTO create(CreateGymScheduleRequest request);
    java.util.List<GymScheduleDTO> getAll();
    GymScheduleDTO update(Integer id, CreateGymScheduleRequest request);
    void delete(Integer id);
}
