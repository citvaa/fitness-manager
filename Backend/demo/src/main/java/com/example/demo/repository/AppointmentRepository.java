package com.example.demo.repository;

import com.example.demo.model.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Integer> {
    boolean existsByClientAppointmentsClientIdAndDateAndStartTimeLessThanEqualAndEndTimeGreaterThanEqual(Integer clientId,
                                                                                                         LocalDate date,
                                                                                                         LocalTime endTime,
                                                                                                         LocalTime startTime);
}
