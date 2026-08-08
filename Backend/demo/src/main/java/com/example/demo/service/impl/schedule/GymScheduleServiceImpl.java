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

import java.util.List;

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
        GymSchedule schedule = gymScheduleRepository.findByDay(request.getDay())
                .orElseGet(() -> GymSchedule.builder().day(request.getDay()).build());

        schedule.setOpeningTime(request.getStartTime());
        schedule.setClosingTime(request.getEndTime());

        GymSchedule savedSchedule = gymScheduleRepository.save(schedule);

        return gymScheduleMapper.toDto(savedSchedule);
    }

    public List<GymScheduleDTO> getAll() {
        return gymScheduleMapper.toDto(gymScheduleRepository.findAllByOrderByDayAsc());
    }
}
