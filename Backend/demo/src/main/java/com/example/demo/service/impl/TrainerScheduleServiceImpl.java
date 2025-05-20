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
import com.example.demo.service.HolidayScheduleService;
import com.example.demo.service.TrainerScheduleService;
import com.example.demo.service.params.request.Schedule.CreateTrainerScheduleRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;

@RequiredArgsConstructor
@Service
public class TrainerScheduleServiceImpl implements TrainerScheduleService {
    private final GymScheduleRepository gymScheduleRepository;
    private final TrainerRepository trainerRepository;
    private final TrainerScheduleRepository trainerScheduleRepository;
    private final TrainerScheduleMapper trainerScheduleMapper;
    private final HolidayScheduleService holidayScheduleService;

    @Override
    public TrainerScheduleDTO create(CreateTrainerScheduleRequest request) {
        Integer trainerId = request.getTrainerId();
        LocalDate date = request.getDate();
        LocalTime startTime = request.getStartTime();
        LocalTime endTime = request.getEndTime();
        WorkStatus status = request.getStatus();

        GymSchedule gymSchedule = gymScheduleRepository.findByDay(date.getDayOfWeek())
                .orElseThrow(() -> new RuntimeException("No gym schedule found for " + date));

        if (holidayScheduleService.isGymClosedOn(date)) {
            throw new IllegalArgumentException("Gym is closed on " + date);
        }

        if (startTime.isBefore(gymSchedule.getOpeningTime()) || endTime.isAfter(gymSchedule.getClosingTime())) {
            throw new IllegalArgumentException("Trainer schedule must be within gym hours: " + gymSchedule.getOpeningTime() + " - " + gymSchedule.getClosingTime());
        }

        boolean overlapExists = trainerScheduleRepository.existsByTrainerIdAndDateAndTimeRange(trainerId, date, startTime, endTime);
        if (overlapExists) {
            throw new IllegalArgumentException("Trainer already has a shift overlapping with this time range");
        }

        Trainer trainer = trainerRepository.findById(trainerId).orElseThrow(() -> new RuntimeException("Trainer not found"));

        TrainerSchedule trainerSchedule = new TrainerSchedule();
        trainerSchedule.setTrainer(trainer);
        trainerSchedule.setDate(date);
        trainerSchedule.setStartTime(startTime);
        trainerSchedule.setEndTime(endTime);
        trainerSchedule.setStatus(status);

        TrainerSchedule savedTrainerSchedule = trainerScheduleRepository.save(trainerSchedule);
        return trainerScheduleMapper.toDto(savedTrainerSchedule);
    }
}
