package com.example.demo.service.impl;

import com.example.demo.dto.AppointmentDTO;
import com.example.demo.enums.WorkStatus;
import com.example.demo.mapper.AppointmentMapper;
import com.example.demo.mapper.SessionMapper;
import com.example.demo.model.gym.Room;
import com.example.demo.model.*;
import com.example.demo.model.schedule.GymSchedule;
import com.example.demo.model.schedule.TrainerSchedule;
import com.example.demo.model.user.Client;
import com.example.demo.model.user.ClientAppointment;
import com.example.demo.model.user.ClientSessionTracking;
import com.example.demo.model.user.Trainer;
import com.example.demo.repository.*;
import com.example.demo.repository.schedule.GymScheduleRepository;
import com.example.demo.repository.schedule.TrainerScheduleRepository;
import com.example.demo.repository.gym.RoomRepository;
import com.example.demo.repository.user.ClientAppointmentRepository;
import com.example.demo.repository.user.ClientRepository;
import com.example.demo.repository.user.ClientSessionTrackingRepository;
import com.example.demo.repository.user.TrainerRepository;
import com.example.demo.service.AppointmentService;
import com.example.demo.service.notification.NotificationService;
import com.example.demo.service.params.request.appointment.CreateAppointmentRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.util.Pair;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AppointmentServiceImpl implements AppointmentService {

    private final SessionRepository sessionRepository;
    private final TrainerRepository trainerRepository;
    private final ClientRepository clientRepository;
    private final AppointmentRepository appointmentRepository;
    private final AppointmentMapper appointmentMapper;
    private final SessionMapper sessionMapper;
    private final RoomRepository roomRepository;
    private final GymScheduleRepository gymScheduleRepository;
    private final TrainerScheduleRepository trainerScheduleRepository;
    private final ClientSessionTrackingRepository clientSessionTrackingRepository;
    private final NotificationService notificationService;
    private final ClientAppointmentRepository clientAppointmentRepository;

    @Transactional
    public AppointmentDTO create(@NotNull CreateAppointmentRequest request) throws JsonProcessingException {
        validateAppointment(request);

        Session session = fetchSession(request.getSessionId());
        Trainer trainer = fetchTrainer(request.getTrainerId());
        Room room = request.getRoomId() == null ? null : roomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new EntityNotFoundException("Room not found"));

        Appointment appointment = Appointment.builder()
                .date(request.getDate())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .session(session)
                .trainer(trainer)
                .room(room)
                .build();

        appointment.setClientAppointments(createClientAppointments(request.getClientIds(), appointment));

        AppointmentDTO appointmentDTO = appointmentMapper.toDto(appointmentRepository.save(appointment));

        if (trainer != null) {
            notificationService.sendTrainerAssignmentNotification(trainer.getId(), appointmentDTO);
        }

        return appointmentDTO;
    }

    public List<com.example.demo.dto.SessionDTO> getSessions() {
        return sessionRepository.findAll().stream().map(sessionMapper::toDto).toList();
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
    public AppointmentDTO removeTrainer(Integer id) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Appointment not found"));

        appointment.setTrainer(null);

        return appointmentMapper.toDto(appointmentRepository.save(appointment));
    }

    @Transactional
    public AppointmentDTO addClients(Integer appointmentId, @NotNull Set<Integer> clientIds) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new EntityNotFoundException("Appointment not found"));

        long newClients = clientIds.stream().filter(id -> appointment.getClientAppointments().stream()
                .noneMatch(link -> link.getClient().getId().equals(id))).count();
        if (appointment.getClientAppointments().size() + newClients > appointment.getSession().getMaxParticipants()) {
            throw new IllegalStateException("Appointment capacity would be exceeded");
        }
        Set<Integer> missingIds = clientIds.stream().filter(id -> appointment.getClientAppointments().stream()
                .noneMatch(link -> link.getClient().getId().equals(id))).collect(Collectors.toSet());
        validateClientAvailability(missingIds, appointment.getDate(), appointment.getStartTime(), appointment.getEndTime());
        Set<ClientAppointment> clientAppointments = createClientAppointments(missingIds, appointment);

        appointment.getClientAppointments().addAll(clientAppointments);
        appointmentRepository.save(appointment);

        return appointmentMapper.toDto(appointment);
    }

    @Transactional
    public AppointmentDTO removeClient(Integer appointmentId, Integer clientId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new EntityNotFoundException("Appointment not found"));

        boolean removed = appointment.getClientAppointments().removeIf(clientAppointment -> clientAppointment.getClient().getId().equals(clientId));
        if (!removed) throw new EntityNotFoundException("Client is not assigned to appointment");
        Client client = clientRepository.findById(clientId).orElseThrow(() -> new EntityNotFoundException("Client not found"));
        decrementReservedAppointments(getOrCreateClientSessionTracking(client, appointment.getSession()));

        return appointmentMapper.toDto(appointmentRepository.save(appointment));
    }

    public List<AppointmentDTO> getAvailable() {
        return appointmentRepository.findAll().stream()
                .filter(this::isFuture)
                .filter(appointment -> appointment.getClientAppointments().size() < appointment.getSession().getMaxParticipants())
                .sorted(java.util.Comparator.comparing(Appointment::getDate).thenComparing(Appointment::getStartTime))
                .map(appointmentMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public AppointmentDTO reserve(Integer id) {
        Client client = getAuthenticatedClient();

        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Appointment not found!"));

        if (appointment.getClientAppointments().size() >= appointment.getSession().getMaxParticipants()) {
            throw new RuntimeException("No available spots for this appointment!");
        }

        if (!isFuture(appointment)) {
            throw new IllegalArgumentException("Only future appointments can be reserved!");
        }
        if (appointment.getClientAppointments().stream().anyMatch(link -> link.getClient().getId().equals(client.getId()))) {
            throw new IllegalStateException("Client is already registered for this appointment!");
        }

        ClientAppointment clientAppointment = createClientAppointment(client, appointment);
        appointment.getClientAppointments().add(clientAppointment);

        return appointmentMapper.toDto(appointmentRepository.save(appointment));
    }

    @Transactional
    public AppointmentDTO cancel(Integer id) {
        Client client = getAuthenticatedClient();

        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Appointment not found"));

        LocalDateTime appointmentTime = LocalDateTime.of(appointment.getDate(), appointment.getStartTime());
        LocalDateTime cancellationDeadline = appointmentTime.minusHours(24);

        if (LocalDateTime.now().isBefore(cancellationDeadline)) {
            boolean removed = appointment.getClientAppointments().removeIf(clientAppointment ->
                    clientAppointment.getClient().getId().equals(client.getId()));

            if (!removed) {
                throw new RuntimeException("Client is not registered for this appointment!");
            }

            ClientSessionTracking tracking = getOrCreateClientSessionTracking(client, appointment.getSession());
            decrementReservedAppointments(tracking);

            return appointmentMapper.toDto(appointmentRepository.save(appointment));
        } else {
            throw new RuntimeException("Too late to cancel! Cancellation must be at least 24 hours before the appointment.");
        }
    }

    public List<AppointmentDTO> getAllWithoutTrainer() {
        return appointmentRepository.findAll().stream()
                .filter(this::isFuture)
                .filter(appointment -> appointment.getTrainer() == null)
                .sorted(java.util.Comparator.comparing(Appointment::getDate).thenComparing(Appointment::getStartTime))
                .map(appointmentMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public AppointmentDTO assign(Integer appointmentId) {
        Pair<Trainer, Appointment> trainerAppointment = getAuthenticatedTrainerAndAppointment(appointmentId);
        if (!isFuture(trainerAppointment.getSecond())) {
            throw new IllegalArgumentException("Only future appointments can be assigned!");
        }
        if (trainerAppointment.getSecond().getTrainer() != null) {
            throw new IllegalStateException("Appointment already has an assigned trainer!");
        }
        trainerAppointment.getSecond().setTrainer(trainerAppointment.getFirst());
        return appointmentMapper.toDto(appointmentRepository.save(trainerAppointment.getSecond()));
    }

    @Transactional
    public AppointmentDTO unassign(Integer appointmentId) {
        Pair<Trainer, Appointment> trainerAppointment = getAuthenticatedTrainerAndAppointment(appointmentId);
        if (!isFuture(trainerAppointment.getSecond())) {
            throw new IllegalArgumentException("Only future appointments can be unassigned!");
        }
        if (!trainerAppointment.getFirst().equals(trainerAppointment.getSecond().getTrainer())) {
            throw new RuntimeException("Trainer is not assigned to this appointment!");
        }
        trainerAppointment.getSecond().setTrainer(null);
        return appointmentMapper.toDto(appointmentRepository.save(trainerAppointment.getSecond()));
    }

    public List<AppointmentDTO> getMyAppointments() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new AccessDeniedException("Unauthorized access!");
        }
        String email = jwt.getClaim("email");
        if (jwt.getClaimAsStringList("roles").contains("TRAINER")) {
            Trainer trainer = trainerRepository.findByUserEmail(email)
                    .orElseThrow(() -> new AccessDeniedException("Trainer profile not found"));
            return appointmentMapper.toDto(appointmentRepository.findByTrainerIdOrderByDateDescStartTimeDesc(trainer.getId()));
        }
        Client client = clientRepository.findByUserEmail(email)
                .orElseThrow(() -> new AccessDeniedException("Client profile not found"));
        return appointmentMapper.toDto(appointmentRepository.findDistinctByClientAppointmentsClientIdOrderByDateDescStartTimeDesc(client.getId()));
    }

    private boolean isFuture(Appointment appointment) {
        return LocalDateTime.of(appointment.getDate(), appointment.getStartTime()).isAfter(LocalDateTime.now());
    }

    public List<AppointmentDTO> getAppointmentsForTrainer(Integer trainerId, LocalDate date) {
        return appointmentRepository.findByTrainerIdAndDate(trainerId, date)
                .stream()
                .map(appointmentMapper::toDto)
                .toList();
    }

    public Optional<AppointmentDTO> getAppointmentForClient(Integer clientId, LocalDate date) {
        return clientAppointmentRepository.findByClientIdAndAppointmentDate(clientId, date)
                .stream()
                .findFirst()
                .map(clientAppointment -> appointmentMapper.toDto(clientAppointment.getAppointment()));
    }

    public List<Appointment> findAppointmentsStartingBetween(LocalTime start, LocalTime end, LocalDate date) {
        return appointmentRepository.findByStartTimeBetweenAndDate(start, end, date);
    }









    private void validateAppointment(@NotNull CreateAppointmentRequest request) {
        validateDateAndTimeRange(request.getDate(), request.getStartTime(), request.getEndTime());
        validateGymSchedule(request.getDate(), request.getStartTime(), request.getEndTime());
        validateTrainerAvailability(request.getTrainerId(), request.getDate(), request.getStartTime(), request.getEndTime());
        validateClientAvailability(request.getClientIds(), request.getDate(), request.getStartTime(), request.getEndTime());
    }

    private void validateDateAndTimeRange(@NotNull LocalDate date, @NotNull LocalTime startTime, @NotNull LocalTime endTime) {
        if (startTime.isAfter(endTime) || startTime.equals(endTime)) {
            throw new IllegalArgumentException("Start time must be before end time!");
        }

        if (date.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Appointment date must be in the future!");
        }
    }

    private void validateGymSchedule(@NotNull LocalDate date, @NotNull LocalTime startTime, LocalTime endTime) {
        GymSchedule gymSchedule = gymScheduleRepository.findByDay(date.getDayOfWeek())
                .orElseThrow(() -> new IllegalArgumentException("No gym schedule found for " + date.getDayOfWeek()));

        if (startTime.isBefore(gymSchedule.getOpeningTime()) || endTime.isAfter(gymSchedule.getClosingTime())) {
            throw new IllegalArgumentException("Appointment is outside gym working hours!");
        }
    }

    private void validateTrainerAvailability(Integer id, LocalDate date, LocalTime startTime, LocalTime endTime) {
        if (id != null && !isTrainerAvailable(id, date, startTime, endTime)) {
            throw new IllegalArgumentException("Trainer with ID " + id + " is already occupied in this time slot!");
        }
    }

    private void validateClientAvailability(Set<Integer> ids, LocalDate date, LocalTime startTime, LocalTime endTime) {
        if (ids != null) {
            for (Integer clientId : ids) {
                if (!isClientAvailable(clientId, date, startTime, endTime)) {
                    throw new IllegalArgumentException("Client with ID " + clientId + " is already booked in this time slot!");
                }
            }
        }
    }

    private boolean isClientAvailable(Integer id, LocalDate date, LocalTime startTime, LocalTime endTime) {
        return !appointmentRepository.existsByClientAppointmentsClientIdAndDateAndStartTimeLessThanEqualAndEndTimeGreaterThanEqual(
                id, date, endTime, startTime);
    }

    private boolean isTrainerAvailable(Integer id, @NotNull LocalDate date, LocalTime startTime, LocalTime endTime) {
        List<TrainerSchedule> schedules = trainerScheduleRepository.findByTrainerIdAndDate(id, date);

        return schedules.stream()
                .filter(schedule -> schedule.getStatus() == WorkStatus.WORKING)
                .anyMatch(schedule ->
                        (startTime.equals(schedule.getStartTime()) || startTime.isAfter(schedule.getStartTime())) &&
                                (endTime.equals(schedule.getEndTime()) || endTime.isBefore(schedule.getEndTime()))
                );
    }

    private Session fetchSession(Integer id) {
        return sessionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Session not found"));
    }

    private Trainer fetchTrainer(Integer id) {
        return (id != null) ? trainerRepository.findById(id).orElse(null) : null;
    }

    private Set<ClientAppointment> createClientAppointments(Set<Integer> clientIds, Appointment appointment) {
        if (clientIds == null || clientIds.isEmpty()) {
            return Collections.emptySet();
        }

        return clientIds.stream()
                .map(clientId -> {
                    Client client = clientRepository.findById(clientId)
                            .orElseThrow(() -> new EntityNotFoundException("Client not found"));

                    return createClientAppointment(client, appointment);
                })
                .collect(Collectors.toSet());
    }

    private ClientSessionTracking getOrCreateClientSessionTracking(Client client, Session session) {
        return clientSessionTrackingRepository.findByClientAndSession(client, session)
                .orElseGet(() -> ClientSessionTracking.builder()
                        .client(client)
                        .session(session)
                        .remainingAppointments(0)
                        .reservedAppointments(0)
                        .build());
    }

    private void incrementReservedAppointments(@NotNull ClientSessionTracking tracking) {
        tracking.setReservedAppointments(tracking.getReservedAppointments() + 1);
        tracking.setRemainingAppointments(tracking.getRemainingAppointments() - 1);
        clientSessionTrackingRepository.save(tracking);
    }

    private void decrementReservedAppointments(@NotNull ClientSessionTracking tracking) {
        tracking.setReservedAppointments(tracking.getReservedAppointments() - 1);
        tracking.setRemainingAppointments(tracking.getRemainingAppointments() + 1);
        clientSessionTrackingRepository.save(tracking);
    }

    private ClientAppointment createClientAppointment(Client client, @NotNull Appointment appointment) {
        ClientSessionTracking tracking = getOrCreateClientSessionTracking(client, appointment.getSession());
        incrementReservedAppointments(tracking);

        return ClientAppointment.builder()
                .client(client)
                .appointment(appointment)
                .build();
    }

    private Client getAuthenticatedClient() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new AccessDeniedException("Unauthorized access!");
        }

        String email = jwt.getClaim("email");

        return clientRepository.findByUserEmail(email)
                .orElseThrow(() -> new RuntimeException("Client not found for the logged-in user!"));
    }

    private @NotNull Pair<Trainer, Appointment> getAuthenticatedTrainerAndAppointment(Integer id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new AccessDeniedException("Unauthorized access!");
        }

        String email = jwt.getClaim("email");

        Trainer trainer = trainerRepository.findByUserEmail(email)
                .orElseThrow(() -> new RuntimeException("Trainer not found for the logged-in user!"));

        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Appointment not found"));

        return Pair.of(trainer, appointment);
    }
}
