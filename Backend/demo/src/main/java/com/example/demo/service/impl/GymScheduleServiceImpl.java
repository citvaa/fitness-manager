package com.example.demo.service.impl;

import com.example.demo.dto.GymScheduleDTO;
import com.example.demo.mapper.GymScheduleMapper;
import com.example.demo.model.GymSchedule;
import com.example.demo.repository.GymScheduleRepository;
import com.example.demo.service.GymScheduleService;
import com.example.demo.service.params.request.Schedule.CreateGymScheduleRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class GymScheduleServiceImpl implements GymScheduleService {

    private final GymScheduleRepository gymScheduleRepository;
    private final GymScheduleMapper gymScheduleMapper;

    @Override
    public GymScheduleDTO create(CreateGymScheduleRequest request) {
        if (gymScheduleRepository.existsByDay(request.getDay())) {
            throw new IllegalArgumentException("Schedule for " + request.getDay() + " already exists");
        }

        GymSchedule schedule = new GymSchedule();
        schedule.setDay(request.getDay());
        schedule.setOpeningTime(request.getStartTime());
        schedule.setClosingTime(request.getEndTime());

        GymSchedule savedSchedule = gymScheduleRepository.save(schedule);

        return gymScheduleMapper.toDto(savedSchedule);
    }
}
