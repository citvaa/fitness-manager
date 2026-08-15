package com.example.demo.repository;

import com.example.demo.model.Appointment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Integer> {
    boolean existsByClientAppointmentsClientIdAndDateAndStartTimeLessThanEqualAndEndTimeGreaterThanEqual(Integer clientId,
                                                                                                         LocalDate date,
                                                                                                         LocalTime endTime,
                                                                                                         LocalTime startTime);

    List<Appointment> findByTrainerIdAndDate(Integer trainerId, LocalDate date);

    List<Appointment> findByTrainerIdOrderByDateDescStartTimeDesc(Integer trainerId);

    List<Appointment> findByStartTimeBetweenAndDate(LocalTime startTime, LocalTime startTime2, LocalDate date);

    @EntityGraph(attributePaths = {
            "trainer",
            "clientAppointments.client.user.userRoles",
            "session"
    })
    List<Appointment> findByDate(LocalDate date);

    List<Appointment> findByRoomIsNotNullAndDateAndStartTimeLessThanEqualAndEndTimeGreaterThan(
            LocalDate date, LocalTime nowInclusive, LocalTime nowExclusive);

    boolean existsByTrainerIdAndClientAppointmentsClientId(Integer trainerId, Integer clientId);

    List<Appointment> findDistinctByClientAppointmentsClientIdOrderByDateDescStartTimeDesc(Integer clientId);

    Optional<Appointment> findFirstByTrainerIdAndDateAndStartTimeLessThanAndEndTimeGreaterThan(
            Integer trainerId, LocalDate date, LocalTime endTime, LocalTime startTime);

    Optional<Appointment> findFirstByRoomIdAndDateAndStartTimeLessThanAndEndTimeGreaterThan(
            Integer roomId, LocalDate date, LocalTime endTime, LocalTime startTime);
}
