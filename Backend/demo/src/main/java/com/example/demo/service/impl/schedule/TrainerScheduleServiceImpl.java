package com.example.demo.service.impl.schedule;

import com.example.demo.dto.schedule.TrainerScheduleDTO;
import com.example.demo.enums.WorkStatus;
import com.example.demo.mapper.schedule.TrainerScheduleMapper;
import com.example.demo.model.schedule.GymSchedule;
import com.example.demo.model.user.Trainer;
import com.example.demo.model.schedule.TrainerSchedule;
import com.example.demo.repository.schedule.GymScheduleRepository;
import com.example.demo.repository.user.TrainerRepository;
import com.example.demo.repository.schedule.TrainerScheduleRepository;
import com.example.demo.service.HolidayService;
import com.example.demo.service.schedule.TrainerScheduleService;
import com.example.demo.service.params.request.schedule.CreateTrainerScheduleRequest;
import com.example.demo.service.params.request.schedule.CreateTrainerUnavailabilityRequest;
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
        validateScheduleRequest(request.getDate(), request.getStartTime(), request.getEndTime());
        validateGymHours(request.getDate(), request.getStartTime(), request.getEndTime());
        validateTrainerAvailability(request.getTrainerId(), request.getDate(), request.getStartTime(), request.getEndTime());

        Trainer trainer = fetchTrainer(request.getTrainerId());
        TrainerSchedule trainerSchedule = buildTrainerSchedule(trainer, request.getDate(), request.getStartTime(), request.getEndTime());

        return trainerScheduleMapper.toDto(trainerScheduleRepository.save(trainerSchedule));
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



    private void validateScheduleRequest(LocalDate date, @NotNull LocalTime startTime, LocalTime endTime) {
        if (startTime.isAfter(endTime)) {
            throw new IllegalArgumentException("Start time is after end time");
        }

        if (date.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Schedule date cannot be in the past!");
        }
    }

    private void validateGymHours(@NotNull LocalDate date, LocalTime startTime, LocalTime endTime) {
        GymSchedule gymSchedule = gymScheduleRepository.findByDay(date.getDayOfWeek())
                .orElseThrow(() -> new RuntimeException("No gym schedule found for " + date));

        if (holidayService.isGymClosedOn(date)) {
            throw new IllegalArgumentException("Gym is closed on " + date);
        }

        if (startTime.isBefore(gymSchedule.getOpeningTime()) || endTime.isAfter(gymSchedule.getClosingTime())) {
            throw new IllegalArgumentException("Trainer schedule must be within gym hours: " + gymSchedule.getOpeningTime() + " - " + gymSchedule.getClosingTime());
        }
    }

    private void validateTrainerAvailability(Integer trainerId, LocalDate date, LocalTime startTime, LocalTime endTime) {
        boolean overlapExists = trainerScheduleRepository.existsByTrainerIdAndDateAndTimeRange(trainerId, date, startTime, endTime);
        if (overlapExists) {
            throw new IllegalArgumentException("Trainer already has a shift overlapping with this time range");
        }
    }

    private Trainer fetchTrainer(Integer trainerId) {
        return trainerRepository.findById(trainerId)
                .orElseThrow(() -> new RuntimeException("Trainer not found"));
    }

    private TrainerSchedule buildTrainerSchedule(Trainer trainer, LocalDate date, LocalTime startTime, LocalTime endTime) {
        return TrainerSchedule.builder()
                .trainer(trainer)
                .date(date)
                .startTime(startTime)
                .endTime(endTime)
                .status(WorkStatus.WORKING)
                .build();
    }
}
