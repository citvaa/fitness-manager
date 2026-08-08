package com.example.demo.service.impl;

import com.example.demo.dto.HolidayDTO;
import com.example.demo.mapper.HolidayMapper;
import com.example.demo.model.Holiday;
import com.example.demo.repository.HolidayRepository;
import com.example.demo.service.params.request.schedule.CreateHolidayRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link HolidayServiceImpl} - previously untested, see AGENTS.md
 * ("Upgrade: Faza 9 decisions"). Unlike {@code GymScheduleServiceImpl.create()}, holiday creation
 * is intentionally insert-only (no upsert/edit support was asked for - see AGENTS.md
 * "Upgrade: Faza 6 decisions"), so there's no update path to test here.
 */
@ExtendWith(MockitoExtension.class)
class HolidayServiceImplTest {

    @Mock
    private HolidayMapper holidayMapper;
    @Mock
    private HolidayRepository holidayRepository;

    private HolidayServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new HolidayServiceImpl(holidayMapper, holidayRepository);
    }

    @Test
    void create_mapsSavesAndReturnsTheHoliday() {
        CreateHolidayRequest request = new CreateHolidayRequest(LocalDate.of(2026, 1, 1), "Nova godina");
        Holiday entity = new Holiday(1, LocalDate.of(2026, 1, 1), "Nova godina");
        when(holidayMapper.toEntity(request)).thenReturn(entity);
        when(holidayRepository.save(entity)).thenReturn(entity);
        when(holidayMapper.toDTO(entity)).thenReturn(new HolidayDTO(1, LocalDate.of(2026, 1, 1), "Nova godina"));

        HolidayDTO result = service.create(request);

        assertThat(result.getDescription()).isEqualTo("Nova godina");
    }

    @Test
    void isGymClosedOn_delegatesToRepository() {
        when(holidayRepository.existsByDate(LocalDate.of(2026, 1, 1))).thenReturn(true);

        assertThat(service.isGymClosedOn(LocalDate.of(2026, 1, 1))).isTrue();
    }

    @Test
    void getAll_returnsHolidaysOrderedByDate() {
        when(holidayRepository.findAllByOrderByDateAsc()).thenReturn(List.of(new Holiday()));
        when(holidayMapper.toDTO(anyList())).thenReturn(List.of(new HolidayDTO()));

        assertThat(service.getAll()).hasSize(1);
    }
}
