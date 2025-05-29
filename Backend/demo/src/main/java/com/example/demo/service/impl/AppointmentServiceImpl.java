package com.example.demo.service.impl;

import com.example.demo.dto.AppointmentDTO;
import com.example.demo.enums.WorkStatus;
import com.example.demo.mapper.AppointmentMapper;
import com.example.demo.model.*;
import com.example.demo.repository.*;
import com.example.demo.service.AppointmentService;
import com.example.demo.service.params.request.Appointment.CreateAppointmentRequest;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AppointmentServiceImpl implements AppointmentService {

    private final SessionRepository sessionRepository;
    private final TrainerRepository trainerRepository;
    private final ClientRepository clientRepository;
    private final AppointmentRepository appointmentRepository;
    private final AppointmentMapper appointmentMapper;
    private final GymScheduleRepository gymScheduleRepository;
    private final TrainerScheduleRepository trainerScheduleRepository;

    @Transactional
    public AppointmentDTO create(@NotNull CreateAppointmentRequest request) {
        LocalDate date = request.getDate();
        LocalTime startTime = request.getStartTime();
        LocalTime endTime = request.getEndTime();

        validateDateAndTimeRange(date, startTime, endTime);
        validateGymSchedule(date, startTime, endTime);
        validateTrainerAvailability(request.getTrainerId(), date, startTime, endTime);
        validateClientAvailability(request.getClientIds(), date, startTime, endTime);

        Session session = fetchSession(request.getSessionId());
        Trainer trainer = fetchTrainer(request.getTrainerId());

        Appointment appointment = Appointment.builder()
                .date(date)
                .startTime(startTime)
                .endTime(endTime)
                .session(session)
                .trainer(trainer)
                .build();

        Set<ClientAppointment> clientAppointments = createClientAppointments(request.getClientIds(), appointment);
        appointment.setClientAppointments(clientAppointments);

        return appointmentMapper.toDto(appointmentRepository.save(appointment));
    }

    @Transactional
    public AppointmentDTO addTrainer(Integer appointmentId, Integer trainerId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new EntityNotFoundException("Appointment not found"));

        Trainer trainer = trainerRepository.findById(trainerId)
                .orElseThrow(() -> new EntityNotFoundException("Trainer not found"));

        appointment.setTrainer(trainer);

        return appointmentMapper.toDto(appointmentRepository.save(appointment));
    }

    @Transactional
    public AppointmentDTO addClients(Integer appointmentId, @NotNull Set<Integer> clientIds) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new EntityNotFoundException("Appointment not found"));

        Set<ClientAppointment> clientAppointments = createClientAppointments(clientIds, appointment);

        appointment.getClientAppointments().addAll(clientAppointments);
        appointmentRepository.save(appointment);

        return appointmentMapper.toDto(appointment);
    }

    @Transactional
    public AppointmentDTO removeTrainer(Integer appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new EntityNotFoundException("Appointment not found"));

        appointment.setTrainer(null);

        return appointmentMapper.toDto(appointmentRepository.save(appointment));
    }

    @Transactional
    public AppointmentDTO removeClient(Integer appointmentId, Integer clientId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new EntityNotFoundException("Appointment not found"));

        appointment.getClientAppointments().removeIf(clientAppointment -> clientAppointment.getClient().getId().equals(clientId));

        return appointmentMapper.toDto(appointmentRepository.save(appointment));
    }



    private void validateDateAndTimeRange(@NotNull LocalDate date, @NotNull LocalTime startTime, LocalTime endTime) {
        if (startTime.isAfter(endTime)) {
            throw new IllegalArgumentException("Start time is after end time");
        }

        if (date.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Appointment date cannot be in the past!");
        }
    }

    private void validateGymSchedule(@NotNull LocalDate date, @NotNull LocalTime startTime, LocalTime endTime) {
        GymSchedule gymSchedule = gymScheduleRepository.findByDay(date.getDayOfWeek())
                .orElseThrow(() -> new IllegalArgumentException("No gym schedule found for " + date.getDayOfWeek()));

        if (startTime.isBefore(gymSchedule.getOpeningTime()) || endTime.isAfter(gymSchedule.getClosingTime())) {
            throw new IllegalArgumentException("Appointment is outside gym working hours!");
        }
    }

    private void validateTrainerAvailability(Integer trainerId, LocalDate date, LocalTime startTime, LocalTime endTime) {
        if (trainerId != null && !isTrainerAvailable(trainerId, date, startTime, endTime)) {
            throw new IllegalArgumentException("Trainer with ID " + trainerId + " is already occupied in this time slot!");
        }
    }

    private void validateClientAvailability(Set<Integer> clientIds, LocalDate date, LocalTime startTime, LocalTime endTime) {
        if (clientIds != null) {
            for (Integer clientId : clientIds) {
                if (!isClientAvailable(clientId, date, startTime, endTime)) {
                    throw new IllegalArgumentException("Client with ID " + clientId + " is already booked in this time slot!");
                }
            }
        }
    }

    private boolean isClientAvailable(Integer clientId, LocalDate date, LocalTime startTime, LocalTime endTime) {
        return !appointmentRepository.existsByClientAppointmentsClientIdAndDateAndStartTimeLessThanEqualAndEndTimeGreaterThanEqual(
                clientId, date, endTime, startTime);
    }

    private boolean isTrainerAvailable(Integer trainerId, @NotNull LocalDate date, LocalTime startTime, LocalTime endTime) {
        return trainerScheduleRepository.findByTrainerIdAndDate(trainerId, date)
                .stream()
                .filter(schedule -> schedule.getStatus() == WorkStatus.WORKING)
                .anyMatch(schedule ->
                        startTime.isAfter(schedule.getStartTime()) || startTime.equals(schedule.getStartTime()) &&
                                endTime.isBefore(schedule.getEndTime()) || endTime.equals(schedule.getEndTime())
                );
    }

    private Session fetchSession(Integer sessionId) {
        return sessionRepository.findById(sessionId)
                .orElseThrow(() -> new EntityNotFoundException("Session not found"));
    }

    private Trainer fetchTrainer(Integer trainerId) {
        return (trainerId != null) ? trainerRepository.findById(trainerId).orElse(null) : null;
    }

    private Set<ClientAppointment> createClientAppointments(Set<Integer> clientIds, Appointment appointment) {
        if (clientIds == null || clientIds.isEmpty()) {
            return Collections.emptySet();
        }

        return clientIds.stream()
                .map(clientId -> {
                    Client client = clientRepository.findById(clientId)
                            .orElseThrow(() -> new EntityNotFoundException("Client not found"));

                    client.setReservedAppointments(client.getReservedAppointments() + 1);
                    client.setRemainingAppointments(client.getRemainingAppointments() - 1);
                    clientRepository.save(client);

                    return ClientAppointment.builder()
                            .client(client)
                            .appointment(appointment)
                            .build();
                })
                .collect(Collectors.toSet());
    }
}
