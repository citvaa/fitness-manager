package com.example.demo.repository.schedule;

import com.example.demo.model.schedule.GymSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.util.Optional;

@Repository
public interface GymScheduleRepository extends JpaRepository<GymSchedule, Integer> {
    boolean existsByDay(DayOfWeek day);

    Optional<GymSchedule> findByDay(DayOfWeek day);
}
