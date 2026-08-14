package com.example.demo.service;

import com.example.demo.dto.AppointmentDTO;
import com.example.demo.dto.gym.RoomDTO;
import com.example.demo.dto.user.TrainerDTO;
import com.example.demo.model.Appointment;
import com.example.demo.service.params.request.appointment.CreateAppointmentRequest;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface AppointmentService {
    AppointmentDTO create(CreateAppointmentRequest request) throws JsonProcessingException;

    /** Generates weekly-recurring instances of {@code request} starting at its {@code date} (same
     * weekday/time/session/trainer/room every week) - see AGENTS.md "Upgrade: fixed weekly
     * appointment decisions" for how far ahead this generates and how per-occurrence conflicts
     * (e.g. the trainer already booked on one specific week) are handled. Returns only the
     * occurrences that were actually created. */
    List<AppointmentDTO> createRecurringWeekly(CreateAppointmentRequest request) throws JsonProcessingException;

    AppointmentDTO addTrainer(Integer appointmentId, Integer trainerId);

    AppointmentDTO removeTrainer(Integer id);

    AppointmentDTO addClients(Integer appointmentId, Set<Integer> clientIds);

    AppointmentDTO removeClient(Integer appointmentId, Integer clientId);

    List<AppointmentDTO> getAvailable();

    /** Every appointment, regardless of capacity/trainer state - backs the MANAGER slot-management
     * screen (see AGENTS.md "Upgrade: Faza 9 decisions"), unlike getAvailable()/getAllWithoutTrainer()
     * which are each filtered for a specific self-service use case. */
    List<AppointmentDTO> getAll();

    AppointmentDTO reserve(Integer id);

    AppointmentDTO cancel(Integer id);

    List<AppointmentDTO> getAllWithoutTrainer();

    AppointmentDTO assign(Integer id);

    AppointmentDTO unassign(Integer id);

    /** The logged-in client's own reserved appointments, past and future - see AGENTS.md "Upgrade: Faza 7 decisions". */
    List<AppointmentDTO> getMyAppointmentsAsClient();

    /** The logged-in trainer's own assigned appointments, past and future - see AGENTS.md "Upgrade: Faza 7 decisions". */
    List<AppointmentDTO> getMyAppointmentsAsTrainer();

    List<AppointmentDTO> getAppointmentsForTrainer(Integer trainerId, LocalDate date);

    Optional<AppointmentDTO> getAppointmentForClient(Integer clientId, LocalDate date);

    List<Appointment> findAppointmentsStartingBetween(LocalTime start, LocalTime end, LocalDate date);

    /** Trainers that both have a WORKING TrainerSchedule shift covering this date/time range and
     * aren't already booked on another overlapping appointment - i.e. exactly the set that would
     * pass validateTrainerWorkingSchedule()/validateTrainerNotDoubleBooked() in create(). Backs the
     * "new appointment" form's trainer picker so a manager can't even select a combination the
     * backend would reject - a UX narrowing, not a new authority; create() still re-validates from
     * scratch (see AGENTS.md "Upgrade: appointment picker filtering decisions"). */
    List<TrainerDTO> getAvailableTrainers(LocalDate date, LocalTime startTime, LocalTime endTime);

    /** Rooms not already booked by another overlapping appointment in this date/time range - same
     * check as validateRoomNotDoubleBooked() in create(), exposed for the same reason as
     * getAvailableTrainers() above. */
    List<RoomDTO> getAvailableRooms(LocalDate date, LocalTime startTime, LocalTime endTime);
}
