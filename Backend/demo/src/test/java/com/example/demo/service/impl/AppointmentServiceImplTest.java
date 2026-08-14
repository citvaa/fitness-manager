package com.example.demo.service.impl;

import com.example.demo.dto.AppointmentDTO;
import com.example.demo.mapper.AppointmentMapper;
import com.example.demo.model.Appointment;
import com.example.demo.model.Session;
import com.example.demo.model.user.Client;
import com.example.demo.model.user.ClientAppointment;
import com.example.demo.model.user.ClientSessionTracking;
import com.example.demo.model.user.Trainer;
import com.example.demo.repository.AppointmentRepository;
import com.example.demo.repository.SessionRepository;
import com.example.demo.repository.gym.RoomRepository;
import com.example.demo.repository.schedule.GymScheduleRepository;
import com.example.demo.repository.schedule.TrainerScheduleRepository;
import com.example.demo.repository.user.ClientAppointmentRepository;
import com.example.demo.repository.user.ClientRepository;
import com.example.demo.repository.user.ClientSessionTrackingRepository;
import com.example.demo.repository.user.TrainerRepository;
import com.example.demo.service.HolidayService;
import com.example.demo.service.notification.NotificationService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link AppointmentServiceImpl} - the Faza 7 marketplace flow (reserve/cancel,
 * assign/unassign, the "my appointments" endpoints) plus the pre-existing available/without-
 * trainer filtering it builds on. See AGENTS.md "Upgrade: Faza 7 decisions".
 */
@ExtendWith(MockitoExtension.class)
class AppointmentServiceImplTest {

    @Mock
    private SessionRepository sessionRepository;
    @Mock
    private TrainerRepository trainerRepository;
    @Mock
    private ClientRepository clientRepository;
    @Mock
    private AppointmentRepository appointmentRepository;
    @Mock
    private AppointmentMapper appointmentMapper;
    @Mock
    private GymScheduleRepository gymScheduleRepository;
    @Mock
    private TrainerScheduleRepository trainerScheduleRepository;
    @Mock
    private ClientSessionTrackingRepository clientSessionTrackingRepository;
    @Mock
    private NotificationService notificationService;
    @Mock
    private ClientAppointmentRepository clientAppointmentRepository;
    @Mock
    private RoomRepository roomRepository;
    @Mock
    private HolidayService holidayService;

    private AppointmentServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AppointmentServiceImpl(sessionRepository, trainerRepository, clientRepository,
                appointmentRepository, appointmentMapper, gymScheduleRepository, trainerScheduleRepository,
                clientSessionTrackingRepository, notificationService, clientAppointmentRepository, roomRepository,
                holidayService);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAsClient(String email) {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("email", email)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(900))
                .build();
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(jwt, null, List.of()));
    }

    private void authenticateAsTrainer(String email) {
        authenticateAsClient(email); // identical shape - only which repository resolves the email differs
    }

    private Session session(int maxParticipants) {
        return new Session(1, com.example.demo.enums.SessionType.GROUP, maxParticipants);
    }

    // ---------- reserve ----------

    @Test
    void reserve_addsClientWhenSpotAvailable() {
        authenticateAsClient("client@gym.com");
        Client client = Client.builder().id(5).build();
        when(clientRepository.findByUserEmail("client@gym.com")).thenReturn(Optional.of(client));

        Session session = session(3);
        Appointment appointment = Appointment.builder().id(10).session(session).clientAppointments(new HashSet<>()).build();
        when(appointmentRepository.findById(10)).thenReturn(Optional.of(appointment));
        when(clientSessionTrackingRepository.findByClientAndSession(client, session)).thenReturn(Optional.empty());
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(appointmentMapper.toDto(any(Appointment.class))).thenReturn(new AppointmentDTO());

        service.reserve(10);

        assertThat(appointment.getClientAppointments()).hasSize(1);
        verify(clientSessionTrackingRepository).save(any(ClientSessionTracking.class));
    }

    @Test
    void reserve_throwsWhenAppointmentIsFull() {
        authenticateAsClient("client@gym.com");
        Client client = Client.builder().id(5).build();
        when(clientRepository.findByUserEmail("client@gym.com")).thenReturn(Optional.of(client));

        Session session = session(1);
        HashSet<ClientAppointment> existing = new HashSet<>();
        existing.add(ClientAppointment.builder().id(1).client(Client.builder().id(99).build()).build());
        Appointment appointment = Appointment.builder().id(10).session(session).clientAppointments(existing).build();
        when(appointmentRepository.findById(10)).thenReturn(Optional.of(appointment));

        assertThatThrownBy(() -> service.reserve(10))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Nema slobodnih mesta");

        verify(appointmentRepository, never()).save(any());
    }

    @Test
    void reserve_throwsWhenUnauthenticated() {
        assertThatThrownBy(() -> service.reserve(10)).isInstanceOf(AccessDeniedException.class);
    }

    // ---------- cancel ----------

    @Test
    void cancel_removesClientAndRefundsTrackingWhenBeforeDeadline() {
        authenticateAsClient("client@gym.com");
        Client client = Client.builder().id(5).build();
        when(clientRepository.findByUserEmail("client@gym.com")).thenReturn(Optional.of(client));

        Session session = session(3);
        HashSet<ClientAppointment> clientAppointments = new HashSet<>();
        clientAppointments.add(ClientAppointment.builder().id(1).client(client).build());
        Appointment appointment = Appointment.builder().id(10).session(session)
                .date(LocalDate.now().plusDays(5)).startTime(LocalTime.of(10, 0)).endTime(LocalTime.of(11, 0))
                .clientAppointments(clientAppointments).build();
        when(appointmentRepository.findById(10)).thenReturn(Optional.of(appointment));

        ClientSessionTracking tracking = ClientSessionTracking.builder()
                .client(client).session(session).remainingAppointments(2).reservedAppointments(1).build();
        when(clientSessionTrackingRepository.findByClientAndSession(client, session)).thenReturn(Optional.of(tracking));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(appointmentMapper.toDto(any(Appointment.class))).thenReturn(new AppointmentDTO());

        service.cancel(10);

        assertThat(appointment.getClientAppointments()).isEmpty();
        assertThat(tracking.getRemainingAppointments()).isEqualTo(3);
        assertThat(tracking.getReservedAppointments()).isEqualTo(0);
    }

    @Test
    void cancel_throwsWhenPastTheTwentyFourHourDeadline() {
        authenticateAsClient("client@gym.com");
        Client client = Client.builder().id(5).build();
        when(clientRepository.findByUserEmail("client@gym.com")).thenReturn(Optional.of(client));

        HashSet<ClientAppointment> clientAppointments = new HashSet<>();
        clientAppointments.add(ClientAppointment.builder().id(1).client(client).build());
        // Starts in 2 hours - inside the 24h cancellation deadline, so cancelling now must fail.
        java.time.LocalDateTime startsAt = java.time.LocalDateTime.now().plusHours(2);
        Appointment appointment = Appointment.builder().id(10).session(session(3))
                .date(startsAt.toLocalDate()).startTime(startsAt.toLocalTime()).endTime(startsAt.toLocalTime().plusHours(1))
                .clientAppointments(clientAppointments).build();
        when(appointmentRepository.findById(10)).thenReturn(Optional.of(appointment));

        assertThatThrownBy(() -> service.cancel(10))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Prekasno za otkazivanje");

        verify(appointmentRepository, never()).save(any());
    }

    @Test
    void cancel_throwsWhenClientNotRegisteredForAppointment() {
        authenticateAsClient("client@gym.com");
        Client client = Client.builder().id(5).build();
        when(clientRepository.findByUserEmail("client@gym.com")).thenReturn(Optional.of(client));

        Appointment appointment = Appointment.builder().id(10).session(session(3))
                .date(LocalDate.now().plusDays(5)).startTime(LocalTime.of(10, 0)).endTime(LocalTime.of(11, 0))
                .clientAppointments(new HashSet<>()).build();
        when(appointmentRepository.findById(10)).thenReturn(Optional.of(appointment));

        assertThatThrownBy(() -> service.cancel(10))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("nije prijavljen");
    }

    // ---------- assign / unassign ----------

    @Test
    void assign_setsCallingTrainerOnTheAppointment() {
        authenticateAsTrainer("trener@gym.com");
        Trainer trainer = Trainer.builder().id(7).build();
        when(trainerRepository.findByUserEmail("trener@gym.com")).thenReturn(Optional.of(trainer));

        Appointment appointment = Appointment.builder().id(10).trainer(null).build();
        when(appointmentRepository.findById(10)).thenReturn(Optional.of(appointment));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(appointmentMapper.toDto(any(Appointment.class))).thenReturn(new AppointmentDTO());

        service.assign(10);

        assertThat(appointment.getTrainer()).isSameAs(trainer);
    }

    @Test
    void unassign_clearsTrainerWhenCallerIsTheAssignedTrainer() {
        authenticateAsTrainer("trener@gym.com");
        Trainer trainer = Trainer.builder().id(7).build();
        when(trainerRepository.findByUserEmail("trener@gym.com")).thenReturn(Optional.of(trainer));

        Appointment appointment = Appointment.builder().id(10).trainer(trainer).build();
        when(appointmentRepository.findById(10)).thenReturn(Optional.of(appointment));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(appointmentMapper.toDto(any(Appointment.class))).thenReturn(new AppointmentDTO());

        service.unassign(10);

        assertThat(appointment.getTrainer()).isNull();
    }

    @Test
    void unassign_throwsWhenCallerIsNotTheAssignedTrainer() {
        authenticateAsTrainer("trener@gym.com");
        // BaseEntity's Lombok-generated equals() only compares its own (version/createdAt/...)
        // fields, never the subclass id (see AGENTS.md "Known issues") - two otherwise-distinct
        // Trainers with all-null audit fields would incorrectly compare equal, so distinguish
        // them by version here the same way to actually exercise the "not the assigned trainer"
        // branch rather than accidentally passing via the equals() bug.
        Trainer caller = Trainer.builder().id(7).build();
        caller.setVersion(7);
        when(trainerRepository.findByUserEmail("trener@gym.com")).thenReturn(Optional.of(caller));

        Trainer someoneElse = Trainer.builder().id(8).build();
        someoneElse.setVersion(8);
        Appointment appointment = Appointment.builder().id(10).trainer(someoneElse).build();
        when(appointmentRepository.findById(10)).thenReturn(Optional.of(appointment));

        assertThatThrownBy(() -> service.unassign(10))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("nije dodeljen");

        verify(appointmentRepository, never()).save(any());
    }

    // ---------- available / without-trainer filtering ----------

    @Test
    void getAvailable_onlyReturnsAppointmentsWithFreeCapacity() {
        // Distinct version numbers: BaseEntity's Lombok-generated equals() only compares its own
        // (version/createdAt/...) fields, never subclass id (see AGENTS.md "Known issues") - two
        // otherwise-distinct Appointments with all-null audit fields would compare equal, which
        // would make Mockito's equals-based verify(never()).toDto(full) below spuriously match
        // the call for "open" too.
        Appointment full = Appointment.builder().id(1).session(session(1))
                .clientAppointments(setOf(ClientAppointment.builder().id(1).build())).build();
        full.setVersion(1);
        Appointment open = Appointment.builder().id(2).session(session(2))
                .clientAppointments(new HashSet<>()).build();
        open.setVersion(2);
        when(appointmentRepository.findAll()).thenReturn(List.of(full, open));
        when(appointmentMapper.toDto(open)).thenReturn(new AppointmentDTO());

        List<AppointmentDTO> result = service.getAvailable();

        assertThat(result).hasSize(1);
        verify(appointmentMapper, never()).toDto(full);
    }

    @Test
    void getAllWithoutTrainer_onlyReturnsUnassignedAppointments() {
        // See the equals()-collision note in getAvailable_onlyReturnsAppointmentsWithFreeCapacity
        // above - same reason for the distinct version numbers here.
        Appointment assigned = Appointment.builder().id(1).trainer(Trainer.builder().id(1).build()).build();
        assigned.setVersion(1);
        Appointment unassigned = Appointment.builder().id(2).trainer(null).build();
        unassigned.setVersion(2);
        when(appointmentRepository.findAll()).thenReturn(List.of(assigned, unassigned));
        when(appointmentMapper.toDto(unassigned)).thenReturn(new AppointmentDTO());

        List<AppointmentDTO> result = service.getAllWithoutTrainer();

        assertThat(result).hasSize(1);
        verify(appointmentMapper, never()).toDto(assigned);
    }

    // ---------- my-appointments ----------

    @Test
    void getMyAppointmentsAsClient_resolvesClientFromJwt() {
        authenticateAsClient("client@gym.com");
        Client client = Client.builder().id(5).build();
        when(clientRepository.findByUserEmail("client@gym.com")).thenReturn(Optional.of(client));
        when(appointmentRepository.findByClientAppointmentsClientIdOrderByDateDescStartTimeDesc(5))
                .thenReturn(List.of(new Appointment()));
        when(appointmentMapper.toDto(any(Appointment.class))).thenReturn(new AppointmentDTO());

        List<AppointmentDTO> result = service.getMyAppointmentsAsClient();

        assertThat(result).hasSize(1);
    }

    @Test
    void getMyAppointmentsAsTrainer_resolvesTrainerFromJwt() {
        authenticateAsTrainer("trener@gym.com");
        Trainer trainer = Trainer.builder().id(7).build();
        when(trainerRepository.findByUserEmail("trener@gym.com")).thenReturn(Optional.of(trainer));
        when(appointmentRepository.findByTrainerIdOrderByDateDescStartTimeDesc(7))
                .thenReturn(List.of(new Appointment()));
        when(appointmentMapper.toDto(any(Appointment.class))).thenReturn(new AppointmentDTO());

        List<AppointmentDTO> result = service.getMyAppointmentsAsTrainer();

        assertThat(result).hasSize(1);
    }

    // ---------- create (MANAGER slot management, Faza 9) ----------

    @Test
    void create_wiresRoomWhenRoomIdProvided() throws Exception {
        LocalDate date = LocalDate.now().plusDays(1);
        com.example.demo.service.params.request.appointment.CreateAppointmentRequest request =
                new com.example.demo.service.params.request.appointment.CreateAppointmentRequest(
                        date, LocalTime.of(10, 0), LocalTime.of(11, 0), 1, 9, 3, null, false);

        com.example.demo.model.schedule.GymSchedule gymSchedule = com.example.demo.model.schedule.GymSchedule.builder()
                .openingTime(LocalTime.of(8, 0)).closingTime(LocalTime.of(22, 0)).build();
        when(gymScheduleRepository.findByDay(date.getDayOfWeek())).thenReturn(Optional.of(gymSchedule));
        when(sessionRepository.findById(1)).thenReturn(Optional.of(session(3)));
        com.example.demo.model.gym.Room room = com.example.demo.model.gym.Room.builder().id(3).build();
        when(roomRepository.findById(3)).thenReturn(Optional.of(room));
        when(trainerRepository.findById(9)).thenReturn(Optional.of(Trainer.builder().id(9).build()));
        com.example.demo.model.schedule.TrainerSchedule workingShift = com.example.demo.model.schedule.TrainerSchedule.builder()
                .status(com.example.demo.enums.WorkStatus.WORKING)
                .startTime(LocalTime.of(8, 0)).endTime(LocalTime.of(22, 0)).build();
        when(trainerScheduleRepository.findByTrainerIdAndDate(9, date)).thenReturn(List.of(workingShift));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(appointmentMapper.toDto(any(Appointment.class))).thenReturn(new AppointmentDTO());

        service.create(request);

        ArgumentCaptor<Appointment> captor = ArgumentCaptor.forClass(Appointment.class);
        verify(appointmentRepository).save(captor.capture());
        assertThat(captor.getValue().getRoom()).isSameAs(room);
    }

    // Trainer and room became mandatory as of the manager-testing round 3 restructure (see
    // AGENTS.md "Upgrade: fixed weekly appointment decisions") - an unassigned trainer/room made
    // occupancy tracking meaningless. These two tests replace the old
    // "create_leavesRoomNullWhenRoomIdOmitted" test, which exercised behavior that is no longer
    // legal.
    @Test
    void create_rejectsMissingRoom() {
        LocalDate date = LocalDate.now().plusDays(1);
        com.example.demo.service.params.request.appointment.CreateAppointmentRequest request =
                new com.example.demo.service.params.request.appointment.CreateAppointmentRequest(
                        date, LocalTime.of(10, 0), LocalTime.of(11, 0), 1, 9, null, null, false);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Soba");
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    void create_rejectsMissingTrainer() {
        LocalDate date = LocalDate.now().plusDays(1);
        com.example.demo.service.params.request.appointment.CreateAppointmentRequest request =
                new com.example.demo.service.params.request.appointment.CreateAppointmentRequest(
                        date, LocalTime.of(10, 0), LocalTime.of(11, 0), 1, null, 3, null, false);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Trener");
        verify(appointmentRepository, never()).save(any());
    }

    // ---------- getAll (MANAGER slot management, Faza 9) ----------

    @Test
    void getAll_returnsEveryAppointmentRegardlessOfState() {
        when(appointmentRepository.findAll()).thenReturn(List.of(new Appointment(), new Appointment()));
        when(appointmentMapper.toDto(anyList())).thenReturn(List.of(new AppointmentDTO(), new AppointmentDTO()));

        assertThat(service.getAll()).hasSize(2);
    }

    // ---------- addClients / removeClient (regression - MANAGER slot management, Faza 9) ----------
    // These two methods existed since Faza 2 but had no frontend caller until Faza 9 gave them
    // one, which is when this real, pre-existing gap was actually noticed. See AGENTS.md
    // ("Upgrade: Faza 9 decisions").

    @Test
    void removeClient_refundsTheClientsSessionTrackingLikeCancelDoes() {
        Client client = Client.builder().id(5).build();
        Session session = session(3);
        ClientAppointment toRemove = ClientAppointment.builder().id(1).client(client).build();
        Appointment appointment = Appointment.builder().id(10).session(session)
                .clientAppointments(setOf(toRemove)).build();
        when(appointmentRepository.findById(10)).thenReturn(Optional.of(appointment));

        ClientSessionTracking tracking = ClientSessionTracking.builder()
                .client(client).session(session).remainingAppointments(2).reservedAppointments(1).build();
        when(clientSessionTrackingRepository.findByClientAndSession(client, session)).thenReturn(Optional.of(tracking));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(appointmentMapper.toDto(any(Appointment.class))).thenReturn(new AppointmentDTO());

        service.removeClient(10, 5);

        assertThat(appointment.getClientAppointments()).isEmpty();
        // The bug: this refund never happened before - the client's credit stayed permanently
        // "spent" even though a MANAGER, not the client, removed them from the appointment.
        assertThat(tracking.getReservedAppointments()).isEqualTo(0);
        assertThat(tracking.getRemainingAppointments()).isEqualTo(3);
        verify(clientSessionTrackingRepository).save(tracking);
    }

    @Test
    void removeClient_doesNotTouchTrackingWhenClientWasNeverOnTheAppointment() {
        Session session = session(3);
        Appointment appointment = Appointment.builder().id(10).session(session)
                .clientAppointments(new HashSet<>()).build();
        when(appointmentRepository.findById(10)).thenReturn(Optional.of(appointment));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(appointmentMapper.toDto(any(Appointment.class))).thenReturn(new AppointmentDTO());

        service.removeClient(10, 999);

        verifyNoInteractions(clientSessionTrackingRepository);
    }

    @Test
    void addClients_rejectsWhenItWouldExceedSessionCapacity() {
        Session session = session(1);
        ClientAppointment existing = ClientAppointment.builder().id(1).client(Client.builder().id(1).build()).build();
        Appointment appointment = Appointment.builder().id(10).session(session)
                .clientAppointments(setOf(existing)).build();
        when(appointmentRepository.findById(10)).thenReturn(Optional.of(appointment));

        assertThatThrownBy(() -> service.addClients(10, Set.of(2)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("kapacitet");

        verify(appointmentRepository, never()).save(any());
        verifyNoInteractions(clientSessionTrackingRepository);
    }

    @Test
    void addClients_ignoresClientsAlreadyOnTheAppointmentInsteadOfDoubleBookingThem() {
        Client alreadyBooked = Client.builder().id(1).build();
        Session session = session(5);
        ClientAppointment existing = ClientAppointment.builder().id(1).client(alreadyBooked).build();
        Appointment appointment = Appointment.builder().id(10).session(session)
                .clientAppointments(setOf(existing)).build();
        when(appointmentRepository.findById(10)).thenReturn(Optional.of(appointment));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(appointmentMapper.toDto(any(Appointment.class))).thenReturn(new AppointmentDTO());

        // Re-adding the same already-booked client (id 1) must be a no-op, not a second charge.
        service.addClients(10, Set.of(1));

        assertThat(appointment.getClientAppointments()).hasSize(1);
        verifyNoInteractions(clientSessionTrackingRepository, clientRepository);
    }

    @Test
    void addClients_incrementsTrackingExactlyOncePerNewClient() {
        // Regression for a second, adjacent bug found while writing the test above:
        // createClientAppointments() used to increment tracking itself AND ALSO call
        // createClientAppointment(), which increments again - double-charging every client added
        // via addClients() (and create()'s initial clientIds). See AGENTS.md
        // ("Upgrade: Faza 9 decisions").
        Client newClient = Client.builder().id(2).build();
        Session session = session(5);
        Appointment appointment = Appointment.builder().id(10).session(session)
                .clientAppointments(new HashSet<>()).build();
        when(appointmentRepository.findById(10)).thenReturn(Optional.of(appointment));
        when(clientRepository.findById(2)).thenReturn(Optional.of(newClient));
        when(clientSessionTrackingRepository.findByClientAndSession(newClient, session)).thenReturn(Optional.empty());
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(appointmentMapper.toDto(any(Appointment.class))).thenReturn(new AppointmentDTO());

        service.addClients(10, Set.of(2));

        ArgumentCaptor<ClientSessionTracking> captor = ArgumentCaptor.forClass(ClientSessionTracking.class);
        verify(clientSessionTrackingRepository, times(1)).save(captor.capture());
        assertThat(captor.getValue().getReservedAppointments()).isEqualTo(1);
        assertThat(captor.getValue().getRemainingAppointments()).isEqualTo(-1);
    }

    @Test
    void addClients_addsOnlyTheNewClientsWhenMixedWithAlreadyBookedOnes() {
        Client alreadyBooked = Client.builder().id(1).build();
        Client newClient = Client.builder().id(2).build();
        Session session = session(5);
        // Distinct version so this pre-existing entry and the freshly-built ClientAppointment
        // don't collide in the HashSet under BaseEntity's id-less equals() (see "Known issues").
        ClientAppointment existing = ClientAppointment.builder().id(1).client(alreadyBooked).build();
        existing.setVersion(1);
        Appointment appointment = Appointment.builder().id(10).session(session)
                .clientAppointments(setOf(existing)).build();
        when(appointmentRepository.findById(10)).thenReturn(Optional.of(appointment));
        when(clientRepository.findById(2)).thenReturn(Optional.of(newClient));
        when(clientSessionTrackingRepository.findByClientAndSession(newClient, session)).thenReturn(Optional.empty());
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(appointmentMapper.toDto(any(Appointment.class))).thenReturn(new AppointmentDTO());

        service.addClients(10, Set.of(1, 2));

        assertThat(appointment.getClientAppointments()).hasSize(2);
        verify(clientRepository, never()).findById(1);
        verify(clientSessionTrackingRepository).save(any(ClientSessionTracking.class));
    }

    private HashSet<ClientAppointment> setOf(ClientAppointment... items) {
        HashSet<ClientAppointment> set = new HashSet<>();
        for (ClientAppointment item : items) {
            set.add(item);
        }
        return set;
    }
}
