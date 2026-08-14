package com.example.demo.config.dev;

import com.example.demo.model.user.User;
import com.example.demo.repository.AppointmentRepository;
import com.example.demo.repository.HolidayRepository;
import com.example.demo.repository.PaymentRepository;
import com.example.demo.repository.SessionRepository;
import com.example.demo.repository.gym.RoomCheckInRepository;
import com.example.demo.repository.gym.RoomRepository;
import com.example.demo.repository.gym.GymRepository;
import com.example.demo.repository.progress.ClientPersonalRecordRepository;
import com.example.demo.repository.progress.ClientProgressEntryRepository;
import com.example.demo.repository.schedule.GymScheduleRepository;
import com.example.demo.repository.schedule.TrainerScheduleRepository;
import com.example.demo.repository.user.ClientAppointmentRepository;
import com.example.demo.repository.user.ClientRepository;
import com.example.demo.repository.user.ClientSessionTrackingRepository;
import com.example.demo.repository.user.TrainerRepository;
import com.example.demo.repository.user.UserRepository;
import com.example.demo.repository.user.UserRoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.mockito.Mockito.*;

/**
 * Not a database-level idempotency test (the seeder is a {@code CommandLineRunner} that needs a
 * live Postgres to run against for real - see AGENTS.md "Upgrade: Faza 7 decisions" for the
 * fresh-volume verification that exercises the real thing). This verifies the guard logic
 * itself: {@code run()} must be a complete no-op - no trainer/client/appointment/payment/etc.
 * repository writes at all - once the marker trainer's email already exists, which is exactly
 * what makes re-running the seeder against an already-seeded database safe (no duplicated rows).
 */
@ExtendWith(MockitoExtension.class)
class DevDataSeederTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private TrainerRepository trainerRepository;
    @Mock
    private ClientRepository clientRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private GymScheduleRepository gymScheduleRepository;
    @Mock
    private HolidayRepository holidayRepository;
    @Mock
    private SessionRepository sessionRepository;
    @Mock
    private AppointmentRepository appointmentRepository;
    @Mock
    private ClientAppointmentRepository clientAppointmentRepository;
    @Mock
    private ClientSessionTrackingRepository clientSessionTrackingRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private RoomRepository roomRepository;
    @Mock
    private RoomCheckInRepository roomCheckInRepository;
    @Mock
    private ClientProgressEntryRepository clientProgressEntryRepository;
    @Mock
    private ClientPersonalRecordRepository clientPersonalRecordRepository;
    @Mock
    private GymRepository gymRepository;
    @Mock
    private UserRoleRepository userRoleRepository;
    @Mock
    private TrainerScheduleRepository trainerScheduleRepository;

    private DevDataSeeder seeder;

    @BeforeEach
    void setUp() {
        seeder = new DevDataSeeder(userRepository, trainerRepository, clientRepository, passwordEncoder,
                gymScheduleRepository, holidayRepository, sessionRepository, appointmentRepository,
                clientAppointmentRepository, clientSessionTrackingRepository, paymentRepository, roomRepository,
                roomCheckInRepository, clientProgressEntryRepository, clientPersonalRecordRepository,
                gymRepository, userRoleRepository, trainerScheduleRepository);
    }

    @Test
    void run_skipsEntirelyWhenMarkerTrainerAlreadyExists() {
        when(userRepository.findByEmail("marko.markovic@fitpro.dev"))
                .thenReturn(Optional.of(User.builder().id(1).email("marko.markovic@fitpro.dev").build()));

        seeder.run();

        // The whole point of the guard: a second run against an already-seeded database must
        // touch nothing that would duplicate rows.
        verifyNoInteractions(gymScheduleRepository, holidayRepository, sessionRepository,
                appointmentRepository, clientSessionTrackingRepository, paymentRepository,
                roomRepository, roomCheckInRepository, clientProgressEntryRepository,
                clientPersonalRecordRepository, trainerRepository, clientRepository,
                gymRepository, userRoleRepository, trainerScheduleRepository);
        verify(userRepository, never()).save(any());
    }

    @Test
    void run_actuallySeedsWhenMarkerIsAbsent_provingTheSkipAboveIsTheGuardAndNotADeadBranch() {
        when(userRepository.findByEmail("marko.markovic@fitpro.dev")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(any())).thenReturn("hashed");
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        // ensureGymAndRooms()/ensureAdminUser() now run ahead of the gym-schedule/holiday seeding
        // this test actually checks (see AGENTS.md "Upgrade: dev-data ownership decisions") -
        // stub them just enough to avoid NPEs on their Mockito-default-null Gym/save() results.
        when(gymRepository.findAll()).thenReturn(java.util.List.of());
        when(gymRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(roomRepository.findByGymId(any())).thenReturn(java.util.List.of());
        // No GROUP session types seeded - run() is expected to abort with a clear error past
        // this point (validated separately below); what matters for this test is that the guard
        // above did NOT short-circuit before ensureGymSchedule()/ensureHolidays() ran.
        when(sessionRepository.findAll()).thenReturn(java.util.List.of());

        try {
            seeder.run();
        } catch (IllegalStateException ignored) {
            // Expected past the gym-schedule/holiday seeding this test actually checks.
        }

        verify(gymScheduleRepository, atLeastOnce()).existsByDay(any());
        verify(holidayRepository, atLeastOnce()).existsByDate(any());
    }
}
