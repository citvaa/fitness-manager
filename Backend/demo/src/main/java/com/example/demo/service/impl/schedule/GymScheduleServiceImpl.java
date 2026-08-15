package com.example.demo.service.impl.schedule;

import com.example.demo.dto.schedule.GymScheduleDTO;
import com.example.demo.mapper.schedule.GymScheduleMapper;
import com.example.demo.model.schedule.GymSchedule;
import com.example.demo.repository.schedule.GymScheduleRepository;
import com.example.demo.service.schedule.GymScheduleService;
import com.example.demo.service.params.request.schedule.CreateGymScheduleRequest;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.time.DayOfWeek;
import java.time.LocalTime;

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
        validateNoAdjacentDayOverlap(request, null);

        GymSchedule schedule = GymSchedule.builder()
                .day(request.getDay())
                .openingTime(request.getStartTime())
                .closingTime(request.getEndTime())
                .build();

        GymSchedule savedSchedule = gymScheduleRepository.save(schedule);

        return gymScheduleMapper.toDto(savedSchedule);
    }

    public List<GymScheduleDTO> getAll() { return gymScheduleRepository.findAll().stream().map(gymScheduleMapper::toDto).toList(); }

    @Transactional
    public GymScheduleDTO update(Integer id, CreateGymScheduleRequest request) {
        GymSchedule schedule = gymScheduleRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Gym schedule not found"));
        if (!schedule.getDay().equals(request.getDay()) && gymScheduleRepository.existsByDay(request.getDay())) throw new IllegalArgumentException("Schedule for " + request.getDay() + " already exists");
        validateNoAdjacentDayOverlap(request, id);
        schedule.setDay(request.getDay()); schedule.setOpeningTime(request.getStartTime()); schedule.setClosingTime(request.getEndTime());
        return gymScheduleMapper.toDto(gymScheduleRepository.save(schedule));
    }

    @Transactional
    public void delete(Integer id) { gymScheduleRepository.deleteById(id); }

    private void validateNoAdjacentDayOverlap(CreateGymScheduleRequest request, Integer ignoredId) {
        DayOfWeek previousDay = request.getDay().minus(1);
        DayOfWeek nextDay = request.getDay().plus(1);
        gymScheduleRepository.findByDay(previousDay)
                .filter(schedule -> ignoredId == null || !schedule.getId().equals(ignoredId))
                .filter(this::isOvernight)
                .filter(schedule -> request.getStartTime().isBefore(schedule.getClosingTime()))
                .ifPresent(schedule -> { throw new IllegalArgumentException(previousDay + " closing time overlaps " + request.getDay() + " opening time"); });
        if (!request.getEndTime().isAfter(request.getStartTime())) {
            gymScheduleRepository.findByDay(nextDay)
                    .filter(schedule -> ignoredId == null || !schedule.getId().equals(ignoredId))
                    .filter(schedule -> schedule.getOpeningTime().isBefore(request.getEndTime()))
                    .ifPresent(schedule -> { throw new IllegalArgumentException(request.getDay() + " closing time overlaps " + nextDay + " opening time"); });
        }
    }

    private boolean isOvernight(GymSchedule schedule) {
        LocalTime opening = schedule.getOpeningTime();
        return !schedule.getClosingTime().isAfter(opening);
    }
}
