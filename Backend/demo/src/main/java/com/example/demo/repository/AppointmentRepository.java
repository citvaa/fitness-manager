package com.example.demo.repository;

import com.example.demo.model.Appointment;
import com.example.demo.model.user.Trainer;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Integer> {

    /** Unassigns a trainer from every appointment they're on - used before deleting a Trainer
     * row (see UserServiceImpl.delete()), since Appointment.trainer is nullable and a deleted
     * trainer must not leave a dangling FK reference behind. */
    @Modifying
    @Query("UPDATE Appointment a SET a.trainer = null WHERE a.trainer = :trainer")
    void clearTrainer(@Param("trainer") Trainer trainer);
    boolean existsByClientAppointmentsClientIdAndDateAndStartTimeLessThanEqualAndEndTimeGreaterThanEqual(Integer clientId,
                                                                                                         LocalDate date,
                                                                                                         LocalTime endTime,
                                                                                                         LocalTime startTime);

    List<Appointment> findByTrainerIdAndDate(Integer trainerId, LocalDate date);

    List<Appointment> findByStartTimeBetweenAndDate(LocalTime startTime, LocalTime startTime2, LocalDate date);

    /** Appointments in a room that are currently in progress (used for computed room occupancy). */
    List<Appointment> findByRoomIdAndDateAndStartTimeLessThanEqualAndEndTimeGreaterThanEqual(Integer roomId, LocalDate date, LocalTime now1, LocalTime now2);

    @EntityGraph(attributePaths = {
            "trainer",
            "clientAppointments.client.user.userRoles",
            "session"
    })
    List<Appointment> findByDate(LocalDate date);

    /** A client's own reserved appointments (past + future) - backs "my appointments" for CLIENT. */
    List<Appointment> findByClientAppointmentsClientIdOrderByDateDescStartTimeDesc(Integer clientId);

    /** A trainer's own assigned appointments (past + future) - backs "my appointments" for TRAINER. */
    List<Appointment> findByTrainerIdOrderByDateDescStartTimeDesc(Integer trainerId);
}
