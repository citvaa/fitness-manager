package com.example.demo.service.impl;

import com.example.demo.dto.AppointmentDTO;
import com.example.demo.mapper.AppointmentMapper;
import com.example.demo.mapper.SessionMapper;
import com.example.demo.repository.gym.RoomRepository;
import com.example.demo.model.Appointment;
import com.example.demo.model.Session;
import com.example.demo.model.user.Client;
import com.example.demo.model.user.ClientAppointment;
import com.example.demo.model.user.ClientSessionTracking;
import com.example.demo.model.user.Trainer;
import com.example.demo.repository.AppointmentRepository;
import com.example.demo.repository.SessionRepository;
import com.example.demo.repository.HolidayRepository;
import com.example.demo.model.schedule.TrainerSchedule;
import com.example.demo.enums.WorkStatus;
import com.example.demo.model.user.User;
import com.example.demo.repository.schedule.GymScheduleRepository;
import com.example.demo.repository.schedule.TrainerScheduleRepository;
import com.example.demo.repository.user.ClientAppointmentRepository;
import com.example.demo.repository.user.ClientRepository;
import com.example.demo.repository.user.ClientSessionTrackingRepository;
import com.example.demo.repository.user.TrainerRepository;
import com.example.demo.service.notification.NotificationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppointmentMarketplaceServiceTest {
    @Mock SessionRepository sessions;
    @Mock TrainerRepository trainers;
    @Mock ClientRepository clients;
    @Mock AppointmentRepository appointments;
    @Mock AppointmentMapper mapper;
    @Mock SessionMapper sessionMapper;
    @Mock RoomRepository rooms;
    @Mock GymScheduleRepository gymSchedules;
    @Mock TrainerScheduleRepository trainerSchedules;
    @Mock HolidayRepository holidays;
    @Mock ClientSessionTrackingRepository trackings;
    @Mock NotificationService notifications;
    @Mock ClientAppointmentRepository clientAppointments;
    AppointmentServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AppointmentServiceImpl(sessions, trainers, clients, appointments, mapper, sessionMapper, rooms, gymSchedules,
                trainerSchedules, holidays, trackings, notifications, clientAppointments);
        lenient().when(appointments.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(mapper.toDto(any(Appointment.class))).thenReturn(new AppointmentDTO());
    }

    @AfterEach void clearSecurityContext() { SecurityContextHolder.clearContext(); }

    @Test
    void clientCanReserveFutureAppointmentAndCreditsAreAdjusted() {
        authenticate("client@example.com", "CLIENT");
        Client client = Client.builder().id(5).build();
        Session session = session(2, 3);
        Appointment appointment = futureAppointment(11, session, null);
        ClientSessionTracking tracking = ClientSessionTracking.builder().client(client).session(session)
                .remainingAppointments(4).reservedAppointments(1).build();
        when(clients.findByUserEmail("client@example.com")).thenReturn(Optional.of(client));
        when(appointments.findById(11)).thenReturn(Optional.of(appointment));
        when(trackings.findByClientAndSession(client, session)).thenReturn(Optional.of(tracking));

        service.reserve(11);

        assertEquals(1, appointment.getClientAppointments().size());
        assertEquals(3, tracking.getRemainingAppointments());
        assertEquals(2, tracking.getReservedAppointments());
        verify(appointments).save(appointment);
    }

    @Test
    void clientCancellationRemovesReservationAndRestoresCredit() {
        authenticate("client@example.com", "CLIENT");
        Client client = Client.builder().id(5).build();
        Session session = session(2, 3);
        Appointment appointment = futureAppointment(12, session, null);
        appointment.setDate(LocalDate.now().plusDays(3));
        appointment.getClientAppointments().add(ClientAppointment.builder().client(client).appointment(appointment).build());
        ClientSessionTracking tracking = ClientSessionTracking.builder().client(client).session(session)
                .remainingAppointments(3).reservedAppointments(2).build();
        when(clients.findByUserEmail("client@example.com")).thenReturn(Optional.of(client));
        when(appointments.findById(12)).thenReturn(Optional.of(appointment));
        when(trackings.findByClientAndSession(client, session)).thenReturn(Optional.of(tracking));

        service.cancel(12);

        assertTrue(appointment.getClientAppointments().isEmpty());
        assertEquals(4, tracking.getRemainingAppointments());
        assertEquals(1, tracking.getReservedAppointments());
    }

    @Test
    void trainerCanAssignAndUnassignOnlyOwnFutureMarketplaceSlot() {
        authenticate("trainer@example.com", "TRAINER");
        Trainer trainer = Trainer.builder().id(7).user(User.builder().email("trainer@example.com").build()).build();
        Appointment appointment = futureAppointment(13, session(2, 3), null);
        when(trainers.findByUserEmail("trainer@example.com")).thenReturn(Optional.of(trainer));
        when(trainers.findById(7)).thenReturn(Optional.of(trainer));
        TrainerSchedule shift = TrainerSchedule.builder().startTime(LocalTime.of(8,0)).endTime(LocalTime.of(16,0)).status(WorkStatus.WORKING).build();
        when(trainerSchedules.findByTrainerIdAndDate(eq(7), any())).thenReturn(List.of(shift));
        when(appointments.findFirstByTrainerIdAndDateAndStartTimeLessThanAndEndTimeGreaterThan(eq(7), any(), any(), any())).thenReturn(Optional.empty());
        when(appointments.findById(13)).thenReturn(Optional.of(appointment));

        service.assign(13);
        assertSame(trainer, appointment.getTrainer());
        service.unassign(13);
        assertNull(appointment.getTrainer());
        verify(appointments, times(2)).save(appointment);
    }

    @Test
    void assigningWithoutCoveringShiftCreatesExactWorkingShift() {
        authenticate("trainer@example.com", "TRAINER");
        Trainer trainer = Trainer.builder().id(7).user(User.builder().email("trainer@example.com").build()).build();
        Appointment appointment = futureAppointment(14, session(2, 3), null);
        when(trainers.findByUserEmail("trainer@example.com")).thenReturn(Optional.of(trainer));
        when(trainers.findById(7)).thenReturn(Optional.of(trainer));
        when(trainerSchedules.findByTrainerIdAndDate(7, appointment.getDate())).thenReturn(List.of());
        when(trainerSchedules.findOverlapping(7, appointment.getDate(), appointment.getStartTime(), appointment.getEndTime())).thenReturn(List.of());
        when(appointments.findFirstByTrainerIdAndDateAndStartTimeLessThanAndEndTimeGreaterThan(7, appointment.getDate(), appointment.getEndTime(), appointment.getStartTime())).thenReturn(Optional.empty());
        when(appointments.findById(14)).thenReturn(Optional.of(appointment));

        service.assign(14);

        assertSame(trainer, appointment.getTrainer());
        verify(trainerSchedules).save(argThat(shift -> shift.getTrainer() == trainer
                && shift.getDate().equals(appointment.getDate())
                && shift.getStartTime().equals(appointment.getStartTime())
                && shift.getEndTime().equals(appointment.getEndTime())
                && shift.getStatus() == WorkStatus.WORKING));
    }

    @Test
    void realAppointmentConflictStillBlocksAssignmentBeforeCreatingShift() {
        authenticate("trainer@example.com", "TRAINER");
        Trainer trainer = Trainer.builder().id(7).user(User.builder().email("trainer@example.com").build()).build();
        Appointment open = futureAppointment(15, session(2, 3), null);
        Appointment conflict = futureAppointment(99, session(2, 3), trainer);
        when(trainers.findByUserEmail("trainer@example.com")).thenReturn(Optional.of(trainer));
        when(trainers.findById(7)).thenReturn(Optional.of(trainer));
        when(appointments.findById(15)).thenReturn(Optional.of(open));
        when(appointments.findFirstByTrainerIdAndDateAndStartTimeLessThanAndEndTimeGreaterThan(7, open.getDate(), open.getEndTime(), open.getStartTime())).thenReturn(Optional.of(conflict));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> service.assign(15));

        assertTrue(error.getMessage().contains("already booked"));
        verify(trainerSchedules, never()).save(any());
        verify(appointments, never()).save(open);
    }

    @Test
    void ownAppointmentsUseJwtRoleAndNeverAcceptAProfileId() {
        authenticate("trainer@example.com", "TRAINER");
        Trainer trainer = Trainer.builder().id(7).build();
        when(trainers.findByUserEmail("trainer@example.com")).thenReturn(Optional.of(trainer));
        when(appointments.findByTrainerIdOrderByDateDescStartTimeDesc(7)).thenReturn(List.of());
        when(mapper.toDto(anyList())).thenReturn(List.of());

        assertEquals(List.of(), service.getMyAppointments());
        verify(appointments).findByTrainerIdOrderByDateDescStartTimeDesc(7);
        verify(appointments, never()).findDistinctByClientAppointmentsClientIdOrderByDateDescStartTimeDesc(anyInt());
    }

    @Test
    void marketplaceListsExcludePastAndAlreadyAssignedSlots() {
        Appointment futureOpen = futureAppointment(20, session(2, 3), null);
        Appointment futureAssigned = futureAppointment(21, session(2, 3), Trainer.builder().id(7).build());
        Appointment pastOpen = futureAppointment(22, session(2, 3), null);
        pastOpen.setDate(LocalDate.now().minusDays(1));
        when(appointments.findAll()).thenReturn(List.of(pastOpen, futureAssigned, futureOpen));

        assertEquals(1, service.getAllWithoutTrainer().size());
        verify(mapper).toDto(same(futureOpen));
        verify(mapper, never()).toDto(same(pastOpen));
    }

    private Appointment futureAppointment(int id, Session session, Trainer trainer) {
        return Appointment.builder().id(id).date(LocalDate.now().plusDays(2)).startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(11, 0)).session(session).trainer(trainer).clientAppointments(new HashSet<>()).build();
    }

    private Session session(int id, int maxParticipants) {
        Session session = new Session();
        session.setId(id);
        session.setMaxParticipants(maxParticipants);
        return session;
    }

    private void authenticate(String email, String role) {
        Instant now = Instant.now();
        Jwt jwt = new Jwt("token", now, now.plusSeconds(900), Map.of("alg", "HS256"),
                Map.of("email", email, "roles", new ArrayList<>(List.of(role))));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(jwt, "token"));
    }
}
