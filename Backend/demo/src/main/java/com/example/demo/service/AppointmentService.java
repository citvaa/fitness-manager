package com.example.demo.service;

import com.example.demo.dto.AppointmentDTO;
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
}
