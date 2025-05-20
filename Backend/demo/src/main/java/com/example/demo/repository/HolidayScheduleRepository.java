package com.example.demo.repository;

import com.example.demo.model.HolidaySchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HolidayScheduleRepository extends JpaRepository<HolidaySchedule, Integer> {
}
