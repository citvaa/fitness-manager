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
import com.example.demo.service.HolidayService;
import com.example.demo.service.TrainerScheduleService;
import com.example.demo.service.params.request.Schedule.CreateTrainerScheduleRequest;
import com.example.demo.service.params.request.Schedule.CreateTrainerUnavailabilityRequest;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;

@Transactional(readOnly = true)
@RequiredArgsConstructor
@Service
public class TrainerScheduleServiceImpl implements TrainerScheduleService {
    private final GymScheduleRepository gymScheduleRepository;
    private final TrainerRepository trainerRepository;
    private final TrainerScheduleRepository trainerScheduleRepository;
    private final TrainerScheduleMapper trainerScheduleMapper;
    private final HolidayService holidayService;

    @Transactional
    public TrainerScheduleDTO createSchedule(@NotNull CreateTrainerScheduleRequest request) {
        Integer trainerId = request.getTrainerId();
        LocalDate date = request.getDate();
        LocalTime startTime = request.getStartTime();
        LocalTime endTime = request.getEndTime();

        GymSchedule gymSchedule = gymScheduleRepository.findByDay(date.getDayOfWeek())
                .orElseThrow(() -> new RuntimeException("No gym schedule found for " + date));

        if (holidayService.isGymClosedOn(date)) {
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

        TrainerSchedule trainerSchedule = TrainerSchedule.builder()
                .trainer(trainer)
                .date(date)
                .startTime(startTime)
                .endTime(endTime)
                .status(WorkStatus.WORKING)
                .build();

        TrainerSchedule savedTrainerSchedule = trainerScheduleRepository.save(trainerSchedule);
        return trainerScheduleMapper.toDto(savedTrainerSchedule);
    }

    @Transactional
    public void createUnavailability(@NotNull CreateTrainerUnavailabilityRequest request) {
        Integer trainerId = request.getTrainerId();
        LocalDate startDate = request.getStartDate();
        LocalDate endDate = request.getEndDate();
        WorkStatus status = request.getStatus();

        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date cannot be after end date");
        }

        Trainer trainer = trainerRepository.findById(trainerId).orElseThrow(() -> new EntityNotFoundException("Trainer not found"));

        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            TrainerSchedule schedule = new TrainerSchedule();
            schedule.setTrainer(trainer);
            schedule.setDate(currentDate);
            schedule.setStartTime(LocalTime.of(0, 0));
            schedule.setEndTime(LocalTime.of(23, 59));
            schedule.setStatus(status);

            trainerScheduleRepository.save(schedule);
            currentDate = currentDate.plusDays(1);
        }
    }
}
