package com.example.demo.service.impl;

import com.example.demo.dto.TrainerScheduleDTO;
import com.example.demo.enums.WorkStatus;
import com.example.demo.mapper.TrainerScheduleMapper;
import com.example.demo.model.GymSchedule;
import com.example.demo.model.Trainer;
import com.example.demo.model.TrainerSchedule;
import com.example.demo.repository.GymScheduleRepository;
import com.example.demo.repository.TrainerRepository;
import com.example.demo.repository.TrainerScheduleRepository;
import com.example.demo.service.TrainerScheduleService;
import com.example.demo.service.params.request.Schedule.CreateTrainerScheduleRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalTime;

@RequiredArgsConstructor
@Service
public class TrainerScheduleServiceImpl implements TrainerScheduleService {
    private final GymScheduleRepository gymScheduleRepository;
    private final TrainerRepository trainerRepository;
    private final TrainerScheduleRepository trainerScheduleRepository;
    private final TrainerScheduleMapper trainerScheduleMapper;

    @Override
    public TrainerScheduleDTO create(CreateTrainerScheduleRequest request) {
        DayOfWeek day = request.getCreateScheduleRequest().getDay();
        LocalTime startTime = request.getCreateScheduleRequest().getStartTime();
        LocalTime endTime = request.getCreateScheduleRequest().getEndTime();
        Integer trainerId = request.getTrainerId();
        WorkStatus status = request.getStatus();

        GymSchedule gymSchedule = gymScheduleRepository.findByDay(day)
                .orElseThrow(() -> new RuntimeException("No gym schedule found for " + day));

        if (!(status == WorkStatus.WORKING) && (startTime != null || endTime != null)) {
            throw new IllegalArgumentException("Non-working days should not have working hours");
        }

        if (startTime != null && endTime != null &&
                (startTime.isBefore(gymSchedule.getOpeningTime()) || endTime.isAfter(gymSchedule.getClosingTime()))) {
            throw new IllegalArgumentException("Trainer schedule must be within gym hours: " + gymSchedule.getOpeningTime() + " - " + gymSchedule.getClosingTime());
        }

        Trainer trainer = trainerRepository.findById(trainerId).orElseThrow(() -> new RuntimeException("Trainer not found"));

        TrainerSchedule trainerSchedule = new TrainerSchedule();
        trainerSchedule.setTrainer(trainer);
        trainerSchedule.setDay(day);
        trainerSchedule.setStartTime(startTime);
        trainerSchedule.setEndTime(endTime);
        trainerSchedule.setStatus(status);

        TrainerSchedule savedTrainerSchedule = trainerScheduleRepository.save(trainerSchedule);
        return trainerScheduleMapper.toDto(savedTrainerSchedule);
    }
}
