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
import com.example.demo.service.security.AuthenticatedUserService;
import com.example.demo.exception.ApiException;
import org.springframework.http.HttpStatus;
import com.example.demo.service.schedule.TrainerScheduleService;
import com.example.demo.service.params.request.schedule.CreateTrainerScheduleRequest;
import com.example.demo.service.params.request.schedule.CreateTrainerUnavailabilityRequest;
import com.example.demo.service.params.response.schedule.RecurringScheduleResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Transactional(readOnly = true)
@RequiredArgsConstructor
@Service
public class TrainerScheduleServiceImpl implements TrainerScheduleService {
    private static final int RECURRING_WEEKS = 8;
    private final GymScheduleRepository gymScheduleRepository;
    private final TrainerRepository trainerRepository;
    private final TrainerScheduleRepository trainerScheduleRepository;
    private final TrainerScheduleMapper trainerScheduleMapper;
    private final HolidayService holidayService;
    private final AuthenticatedUserService authenticatedUserService;

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

    public List<TrainerScheduleDTO> getByTrainer(Integer trainerId) { return trainerScheduleRepository.findByTrainerIdOrderByDateAscStartTimeAsc(trainerId).stream().map(trainerScheduleMapper::toDto).toList(); }
    public List<TrainerScheduleDTO> getOwn() { return getByTrainer(authenticatedUserService.trainer().getId()); }

    @Transactional
    public TrainerScheduleDTO createOwnSchedule(CreateTrainerScheduleRequest request) {
        request.setTrainerId(authenticatedUserService.trainer().getId());
        return createSchedule(request);
    }

    @Transactional
    public RecurringScheduleResponse createRecurring(CreateTrainerScheduleRequest request) {
        int created = 0;
        List<String> reasons = new java.util.ArrayList<>();
        LocalDate originalDate = request.getDate();
        for (int week = 0; week < RECURRING_WEEKS; week++) {
            request.setDate(originalDate.plusWeeks(week));
            try { createSchedule(request); created++; }
            catch (RuntimeException exception) { reasons.add(request.getDate() + ": " + exception.getMessage()); }
        }
        request.setDate(originalDate);
        if (created == 0) throw new IllegalArgumentException("Nijedna smena nije kreirana:\n" + String.join("\n", reasons));
        return new RecurringScheduleResponse(created, reasons);
    }

    @Transactional
    public RecurringScheduleResponse createOwnRecurring(CreateTrainerScheduleRequest request) {
        request.setTrainerId(authenticatedUserService.trainer().getId());
        return createRecurring(request);
    }

    @Transactional
    public void createOwnUnavailability(CreateTrainerUnavailabilityRequest request) {
        request.setTrainerId(authenticatedUserService.trainer().getId());
        createUnavailability(request);
    }

    @Transactional
    public TrainerScheduleDTO update(Integer id, CreateTrainerScheduleRequest request, boolean ownOnly) {
        TrainerSchedule existing = trainerScheduleRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Trainer schedule not found"));
        if (ownOnly && !existing.getTrainer().getId().equals(authenticatedUserService.trainer().getId())) throw new ApiException(HttpStatus.FORBIDDEN, "You can only change your own schedule");
        Integer trainerId = ownOnly ? existing.getTrainer().getId() : request.getTrainerId();
        validateScheduleRequest(request.getDate(), request.getStartTime(), request.getEndTime());
        validateGymHours(request.getDate(), request.getStartTime(), request.getEndTime());
        existing.setTrainer(fetchTrainer(trainerId)); existing.setDate(request.getDate()); existing.setStartTime(request.getStartTime()); existing.setEndTime(request.getEndTime()); existing.setStatus(WorkStatus.WORKING);
        return trainerScheduleMapper.toDto(trainerScheduleRepository.save(existing));
    }

    @Transactional
    public void delete(Integer id, boolean ownOnly) {
        TrainerSchedule existing = trainerScheduleRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Trainer schedule not found"));
        if (ownOnly && !existing.getTrainer().getId().equals(authenticatedUserService.trainer().getId())) throw new ApiException(HttpStatus.FORBIDDEN, "You can only delete your own schedule");
        trainerScheduleRepository.delete(existing);
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
