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

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Transactional(readOnly = true)
@RequiredArgsConstructor
@Service
public class GymScheduleServiceImpl implements GymScheduleService {

    private final GymScheduleRepository gymScheduleRepository;
    private final GymScheduleMapper gymScheduleMapper;

    /**
     * Upsert per day-of-week (see AGENTS.md "Upgrade: Faza 6 decisions") - a manager fixing a
     * typo in already-entered gym hours would otherwise be permanently blocked by the original
     * existsByDay() guard, since there was never an update endpoint for this entity.
     */
    @Transactional
    public GymScheduleDTO create(@NotNull CreateGymScheduleRequest request) {
        validateNoAdjacentDayOverlap(request.getDay(), request.getStartTime(), request.getEndTime());

        GymSchedule schedule = gymScheduleRepository.findByDay(request.getDay())
                .orElseGet(() -> GymSchedule.builder().day(request.getDay()).build());

        schedule.setOpeningTime(request.getStartTime());
        schedule.setClosingTime(request.getEndTime());

        GymSchedule savedSchedule = gymScheduleRepository.save(schedule);

        return gymScheduleMapper.toDto(savedSchedule);
    }

    /**
     * A day's hours are allowed to cross midnight (closingTime <= openingTime just means "closes
     * the next calendar day at this time" - see AGENTS.md, this phase's "manager-testing fixes"),
     * but the overnight portion must not overlap the neighboring day's own hours - e.g. Thursday
     * open until 02:00 must not overlap Friday opening at 01:00. Checked in both directions
     * (this day reaching into the next, and the previous day reaching into this one) since either
     * neighbor could be the one already saved.
     */
    private void validateNoAdjacentDayOverlap(@NotNull DayOfWeek day, @NotNull LocalTime openingTime, @NotNull LocalTime closingTime) {
        if (!closingTime.isAfter(openingTime)) {
            Optional<GymSchedule> nextDay = gymScheduleRepository.findByDay(day.plus(1));
            if (nextDay.isPresent() && closingTime.isAfter(nextDay.get().getOpeningTime())) {
                throw new IllegalArgumentException("Radno vreme za " + day
                        + " (do " + closingTime + " narednog dana) preklapa se sa radnim vremenom za " + day.plus(1)
                        + " (od " + nextDay.get().getOpeningTime() + ")");
            }
        }

        gymScheduleRepository.findByDay(day.minus(1)).ifPresent(previousDay -> {
            if (!previousDay.getClosingTime().isAfter(previousDay.getOpeningTime())
                    && previousDay.getClosingTime().isAfter(openingTime)) {
                throw new IllegalArgumentException("Radno vreme za " + day.minus(1)
                        + " (do " + previousDay.getClosingTime() + " narednog dana) preklapa se sa radnim vremenom za " + day
                        + " (od " + openingTime + ")");
            }
        });
    }

    public List<GymScheduleDTO> getAll() {
        return gymScheduleMapper.toDto(gymScheduleRepository.findAllByOrderByDayAsc());
    }
}
