package com.example.demo.service.impl.schedule;

import com.example.demo.exception.ApiException;
import com.example.demo.mapper.schedule.TrainerScheduleMapper;
import com.example.demo.model.schedule.GymSchedule;
import com.example.demo.model.schedule.TrainerSchedule;
import com.example.demo.model.user.Trainer;
import com.example.demo.repository.schedule.GymScheduleRepository;
import com.example.demo.repository.schedule.TrainerScheduleRepository;
import com.example.demo.repository.user.TrainerRepository;
import com.example.demo.service.HolidayService;
import com.example.demo.service.params.request.schedule.CreateTrainerScheduleRequest;
import com.example.demo.service.security.AuthenticatedUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TrainerScheduleServiceImplTest {
    @Mock GymScheduleRepository gymSchedules;
    @Mock TrainerRepository trainers;
    @Mock TrainerScheduleRepository schedules;
    @Mock TrainerScheduleMapper mapper;
    @Mock HolidayService holidays;
    @Mock AuthenticatedUserService authenticatedUser;
    TrainerScheduleServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new TrainerScheduleServiceImpl(gymSchedules, trainers, schedules, mapper, holidays, authenticatedUser);
    }

    @Test
    void trainerCreatesOnlyOwnWorkingScheduleEvenIfBodyContainsAnotherTrainerId() {
        LocalDate date = LocalDate.now().plusDays(3);
        Trainer ownTrainer = Trainer.builder().id(7).build();
        when(authenticatedUser.trainer()).thenReturn(ownTrainer);
        when(gymSchedules.findByDay(date.getDayOfWeek())).thenReturn(Optional.of(gymHours(date.getDayOfWeek())));
        when(trainers.findById(7)).thenReturn(Optional.of(ownTrainer));
        when(schedules.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.createOwnSchedule(new CreateTrainerScheduleRequest(999, date, LocalTime.of(9, 0), LocalTime.of(17, 0)));

        ArgumentCaptor<TrainerSchedule> saved = ArgumentCaptor.forClass(TrainerSchedule.class);
        verify(schedules).save(saved.capture());
        assertSame(ownTrainer, saved.getValue().getTrainer());
        assertEquals(com.example.demo.enums.WorkStatus.WORKING, saved.getValue().getStatus());
        verify(trainers, never()).findById(999);
    }

    @Test
    void trainerCannotUpdateAnotherTrainersSchedule() {
        Trainer owner = Trainer.builder().id(8).build();
        when(schedules.findById(41)).thenReturn(Optional.of(TrainerSchedule.builder().id(41).trainer(owner).build()));
        when(authenticatedUser.trainer()).thenReturn(Trainer.builder().id(7).build());

        ApiException error = assertThrows(ApiException.class, () -> service.update(41,
                new CreateTrainerScheduleRequest(7, LocalDate.now().plusDays(2), LocalTime.of(9, 0), LocalTime.of(17, 0)), true));

        assertEquals(HttpStatus.FORBIDDEN, error.getStatus());
        verify(schedules, never()).save(any());
    }

    @Test
    void scheduleOutsideGymHoursIsRejectedAsValidationError() {
        LocalDate date = LocalDate.now().plusDays(4);
        when(authenticatedUser.trainer()).thenReturn(Trainer.builder().id(7).build());
        when(gymSchedules.findByDay(date.getDayOfWeek())).thenReturn(Optional.of(gymHours(date.getDayOfWeek())));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> service.createOwnSchedule(
                new CreateTrainerScheduleRequest(null, date, LocalTime.of(5, 0), LocalTime.of(17, 0))));

        assertTrue(error.getMessage().contains("within gym hours"));
        verify(schedules, never()).save(any());
    }

    private GymSchedule gymHours(DayOfWeek day) {
        return GymSchedule.builder().day(day).openingTime(LocalTime.of(6, 0)).closingTime(LocalTime.of(23, 0)).build();
    }
}
