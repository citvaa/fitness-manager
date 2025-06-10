package com.example.demo.service.impl.schedule;

import com.example.demo.dto.schedule.GymScheduleDTO;
import com.example.demo.mapper.GymScheduleMapper;
import com.example.demo.model.schedule.GymSchedule;
import com.example.demo.repository.schedule.GymScheduleRepository;
import com.example.demo.service.schedule.GymScheduleService;
import com.example.demo.service.params.request.schedule.CreateGymScheduleRequest;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
@RequiredArgsConstructor
@Service
public class GymScheduleServiceImpl implements GymScheduleService {

    private final GymScheduleRepository gymScheduleRepository;
    private final GymScheduleMapper gymScheduleMapper;

    @Transactional
    public GymScheduleDTO create(@NotNull CreateGymScheduleRequest request) {
        if (gymScheduleRepository.existsByDay(request.getDay())) {
            throw new IllegalArgumentException("Schedule for " + request.getDay() + " already exists");
        }

        GymSchedule schedule = GymSchedule.builder()
                .day(request.getDay())
                .openingTime(request.getStartTime())
                .closingTime(request.getEndTime())
                .build();

        GymSchedule savedSchedule = gymScheduleRepository.save(schedule);

        return gymScheduleMapper.toDto(savedSchedule);
    }
}
