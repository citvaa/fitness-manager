package com.example.demo.service.impl.schedule;

import com.example.demo.dto.schedule.GymScheduleDTO;
import com.example.demo.mapper.schedule.GymScheduleMapper;
import com.example.demo.model.schedule.GymSchedule;
import com.example.demo.repository.schedule.GymScheduleRepository;
import com.example.demo.service.params.request.schedule.CreateGymScheduleRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link GymScheduleServiceImpl} - the Faza 6 fix turning {@code create()} from
 * insert-only (which permanently locked in a typo, since there was no update endpoint) into an
 * upsert-per-day. See AGENTS.md "Upgrade: Faza 6 decisions".
 */
@ExtendWith(MockitoExtension.class)
class GymScheduleServiceImplTest {

    @Mock
    private GymScheduleRepository gymScheduleRepository;
    @Mock
    private GymScheduleMapper gymScheduleMapper;

    private GymScheduleServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new GymScheduleServiceImpl(gymScheduleRepository, gymScheduleMapper);
    }

    @Test
    void create_insertsANewRowWhenDayHasNoScheduleYet() {
        CreateGymScheduleRequest request = new CreateGymScheduleRequest(
                DayOfWeek.SUNDAY, LocalTime.of(9, 0), LocalTime.of(15, 0));
        when(gymScheduleRepository.findByDay(DayOfWeek.SUNDAY)).thenReturn(Optional.empty());
        when(gymScheduleRepository.save(any(GymSchedule.class))).thenAnswer(inv -> inv.getArgument(0));
        when(gymScheduleMapper.toDto(any(GymSchedule.class))).thenReturn(new GymScheduleDTO());

        service.create(request);

        ArgumentCaptor<GymSchedule> captor = ArgumentCaptor.forClass(GymSchedule.class);
        verify(gymScheduleRepository).save(captor.capture());
        assertThat(captor.getValue().getDay()).isEqualTo(DayOfWeek.SUNDAY);
        assertThat(captor.getValue().getOpeningTime()).isEqualTo(LocalTime.of(9, 0));
        assertThat(captor.getValue().getClosingTime()).isEqualTo(LocalTime.of(15, 0));
    }

    @Test
    void create_overwritesTheExistingRowInsteadOfErroringWhenDayAlreadyHasOne() {
        GymSchedule existing = GymSchedule.builder().id(1).day(DayOfWeek.MONDAY)
                .openingTime(LocalTime.of(0, 0)).closingTime(LocalTime.of(23, 0)).build();
        when(gymScheduleRepository.findByDay(DayOfWeek.MONDAY)).thenReturn(Optional.of(existing));
        when(gymScheduleRepository.save(any(GymSchedule.class))).thenAnswer(inv -> inv.getArgument(0));
        when(gymScheduleMapper.toDto(any(GymSchedule.class))).thenReturn(new GymScheduleDTO());

        CreateGymScheduleRequest request = new CreateGymScheduleRequest(
                DayOfWeek.MONDAY, LocalTime.of(6, 0), LocalTime.of(22, 0));

        service.create(request);

        // Same row (id=1), not a second insert - the fix under test.
        ArgumentCaptor<GymSchedule> captor = ArgumentCaptor.forClass(GymSchedule.class);
        verify(gymScheduleRepository).save(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(1);
        assertThat(captor.getValue().getOpeningTime()).isEqualTo(LocalTime.of(6, 0));
        assertThat(captor.getValue().getClosingTime()).isEqualTo(LocalTime.of(22, 0));
        verify(gymScheduleRepository, times(1)).save(any());
    }

    @Test
    void getAll_returnsScheduleOrderedByDay() {
        when(gymScheduleRepository.findAllByOrderByDayAsc()).thenReturn(java.util.List.of(new GymSchedule()));
        when(gymScheduleMapper.toDto(anyList())).thenReturn(java.util.List.of(new GymScheduleDTO()));

        assertThat(service.getAll()).hasSize(1);
    }
}
