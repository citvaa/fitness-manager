package com.example.demo.config.dev;

import com.example.demo.enums.EmploymentStatus;
import com.example.demo.enums.NotificationPreference;
import com.example.demo.enums.RecordUnit;
import com.example.demo.enums.Role;
import com.example.demo.enums.SessionType;
import com.example.demo.model.Appointment;
import com.example.demo.model.Holiday;
import com.example.demo.model.Payment;
import com.example.demo.model.Session;
import com.example.demo.model.gym.Room;
import com.example.demo.model.gym.RoomCheckIn;
import com.example.demo.model.progress.ClientPersonalRecord;
import com.example.demo.model.progress.ClientProgressEntry;
import com.example.demo.model.schedule.GymSchedule;
import com.example.demo.model.user.Client;
import com.example.demo.model.user.ClientAppointment;
import com.example.demo.model.user.ClientSessionTracking;
import com.example.demo.model.user.Trainer;
import com.example.demo.model.user.User;
import com.example.demo.model.user.UserRole;
import com.example.demo.repository.AppointmentRepository;
import com.example.demo.repository.HolidayRepository;
import com.example.demo.repository.PaymentRepository;
import com.example.demo.repository.SessionRepository;
import com.example.demo.repository.gym.RoomCheckInRepository;
import com.example.demo.repository.gym.RoomRepository;
import com.example.demo.repository.progress.ClientPersonalRecordRepository;
import com.example.demo.repository.progress.ClientProgressEntryRepository;
import com.example.demo.repository.schedule.GymScheduleRepository;
import com.example.demo.repository.user.ClientRepository;
import com.example.demo.repository.user.ClientSessionTrackingRepository;
import com.example.demo.repository.user.TrainerRepository;
import com.example.demo.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Populates a fresh `dev` database with a dataset that looks like a gym that has actually been
 * running for months, rather than the handful of test rows the Flyway dev-data migrations add
 * (see AGENTS.md "Upgrade: Faza 7 decisions" for the full rationale). Deliberately a Java
 * {@link CommandLineRunner}, not a Flyway migration: the whole point is data expressed relative
 * to "now" (weeks of appointment history in the past, a few weeks of bookable slots in the
 * future), which a static SQL migration timestamped at write-time cannot express correctly on
 * every future run.
 *
 * <p>Idempotency: guarded by checking whether {@link #MARKER_EMAIL} (the first trainer this
 * seeder creates) already exists - if so, the whole method is skipped. This mirrors the
 * `WHERE NOT EXISTS` guards the Flyway dev-data migrations use for the same reason (a manager
 * account seeded by an earlier migration, or manual testing, must not be duplicated on restart).
 */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class DevDataSeeder implements CommandLineRunner {

    private static final String DEV_PASSWORD = "password123"; // same known dev password as V1.0017
    private static final String MARKER_EMAIL = "marko.markovic@fitpro.dev";
    private static final int PAST_WEEKS = 8;
    private static final int FUTURE_WEEKS = 3;

    private final UserRepository userRepository;
    private final TrainerRepository trainerRepository;
    private final ClientRepository clientRepository;
    private final PasswordEncoder passwordEncoder;
    private final GymScheduleRepository gymScheduleRepository;
    private final HolidayRepository holidayRepository;
    private final SessionRepository sessionRepository;
    private final AppointmentRepository appointmentRepository;
    private final ClientSessionTrackingRepository clientSessionTrackingRepository;
    private final PaymentRepository paymentRepository;
    private final RoomRepository roomRepository;
    private final RoomCheckInRepository roomCheckInRepository;
    private final ClientProgressEntryRepository clientProgressEntryRepository;
    private final ClientPersonalRecordRepository clientPersonalRecordRepository;

    private final java.util.Random random = new java.util.Random(42); // fixed seed: same-shaped dataset every fresh run

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.findByEmail(MARKER_EMAIL).isPresent()) {
            log.info("🌱 Dev data already seeded (marker trainer {} exists) - skipping.", MARKER_EMAIL);
            return;
        }

        log.info("🌱 Seeding realistic dev data (trainers, clients, appointments, payments, check-ins, progress)...");

        ensureGymSchedule();
        ensureHolidays();

        List<Trainer> trainers = seedTrainers();
        List<Client> clients = seedClients();

        Map<SessionType, List<Session>> byType = sessionRepository.findAll().stream()
                .collect(Collectors.groupingBy(Session::getType));
        Session individual = byType.getOrDefault(SessionType.INDIVIDUAL, List.of()).stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("No INDIVIDUAL session type seeded - cannot seed dev data"));
        List<Session> groupSessions = new ArrayList<>(byType.getOrDefault(SessionType.GROUP, List.of()));
        groupSessions.sort(Comparator.comparing(Session::getMaxParticipants));
        if (groupSessions.size() < 2) {
            throw new IllegalStateException("Expected at least 2 GROUP session types seeded - cannot seed dev data");
        }
        Session smallGroup = groupSessions.get(0);
        Session bigGroup = groupSessions.get(groupSessions.size() - 1);

        List<Room> rooms = roomRepository.findAll();

        // clientId -> sessionId -> {remaining, reserved}, accumulated purely from this run's own
        // simulated payments/reservations, then added on top of whatever a (client, session) row
        // already holds in the database (see persistSessionTracking) - so re-running against a
        // database that already has real usage history on top of the marker check still produces
        // consistent numbers instead of silently duplicating tracking rows.
        Map<Integer, Map<Integer, int[]>> tracking = new HashMap<>();

        seedPayments(clients, individual, smallGroup, bigGroup, tracking);
        seedAppointments(trainers, clients, individual, smallGroup, bigGroup, rooms, tracking);
        persistSessionTracking(tracking, clients, List.of(individual, smallGroup, bigGroup));

        seedRoomCheckIns(clients, rooms);
        seedProgressData(clients);

        log.info("✅ Dev data seeded: {} trainers, {} clients, {} past weeks + {} future weeks of appointments.",
                trainers.size(), clients.size(), PAST_WEEKS, FUTURE_WEEKS);
    }

    // ---------------------------------------------------------------- gym schedule / holidays

    private void ensureGymSchedule() {
        // Monday is already seeded by V1.0009 (00:00-23:00) - existsByDay guards against
        // overwriting it, same "don't touch what's already there" spirit as the migration guards.
        addScheduleIfMissing(DayOfWeek.MONDAY, LocalTime.of(6, 0), LocalTime.of(22, 0));
        addScheduleIfMissing(DayOfWeek.TUESDAY, LocalTime.of(6, 0), LocalTime.of(22, 0));
        addScheduleIfMissing(DayOfWeek.WEDNESDAY, LocalTime.of(6, 0), LocalTime.of(22, 0));
        addScheduleIfMissing(DayOfWeek.THURSDAY, LocalTime.of(6, 0), LocalTime.of(22, 0));
        addScheduleIfMissing(DayOfWeek.FRIDAY, LocalTime.of(6, 0), LocalTime.of(22, 0));
        addScheduleIfMissing(DayOfWeek.SATURDAY, LocalTime.of(8, 0), LocalTime.of(20, 0));
        addScheduleIfMissing(DayOfWeek.SUNDAY, LocalTime.of(9, 0), LocalTime.of(15, 0));
    }

    private void addScheduleIfMissing(DayOfWeek day, LocalTime opening, LocalTime closing) {
        if (gymScheduleRepository.existsByDay(day)) return;
        gymScheduleRepository.save(GymSchedule.builder().day(day).openingTime(opening).closingTime(closing).build());
    }

    private void ensureHolidays() {
        addHolidayIfMissing(LocalDate.now().minusMonths(2), "Godišnjica otvaranja teretane");
        addHolidayIfMissing(nextJan1(), "Novogodišnji praznici");
    }

    private LocalDate nextJan1() {
        LocalDate now = LocalDate.now();
        LocalDate jan1ThisYear = LocalDate.of(now.getYear(), 1, 1);
        return now.isBefore(jan1ThisYear) ? jan1ThisYear : LocalDate.of(now.getYear() + 1, 1, 1);
    }

    private void addHolidayIfMissing(LocalDate date, String description) {
        if (holidayRepository.existsByDate(date)) return;
        Holiday holiday = new Holiday();
        holiday.setDate(date);
        holiday.setDescription(description);
        holidayRepository.save(holiday);
    }

    // ---------------------------------------------------------------------------- trainers/clients

    private List<Trainer> seedTrainers() {
        List<Trainer> trainers = new ArrayList<>();
        trainers.add(createTrainer(MARKER_EMAIL, LocalDate.now().minusYears(3), 1988, EmploymentStatus.FULL_TIME));
        trainers.add(createTrainer("jelena.jovanovic@fitpro.dev", LocalDate.now().minusYears(1).minusMonths(4), 1993, EmploymentStatus.FULL_TIME));
        trainers.add(createTrainer("nikola.nikolic@fitpro.dev", LocalDate.now().minusMonths(8), 1996, EmploymentStatus.CONTRACT));
        // Include the pre-existing Phase 1-6 dev trainer too, so the seeded appointments give
        // "ogi" (the account every earlier phase's docs/screenshots reference) real data as well.
        trainerRepository.findByUserEmail("ogi").ifPresent(trainers::add);
        return trainers;
    }

    private Trainer createTrainer(String email, LocalDate employmentDate, int birthYear, EmploymentStatus status) {
        User user = createActivatedUser(email, Role.TRAINER);
        Trainer trainer = Trainer.builder()
                .user(user)
                .employmentDate(employmentDate)
                .birthYear(birthYear)
                .status(status)
                .build();
        return trainerRepository.save(trainer);
    }

    private List<Client> seedClients() {
        List<Client> clients = new ArrayList<>();
        clients.add(createClient("ana.petrovic@fitpro.dev"));
        clients.add(createClient("stefan.stojanovic@fitpro.dev"));
        clients.add(createClient("milica.ilic@fitpro.dev"));
        clients.add(createClient("petar.pavlovic@fitpro.dev"));
        clients.add(createClient("jovana.jovanovic@fitpro.dev"));
        // Same reasoning as including "ogi" above: the pre-existing dev CLIENT account should
        // also show up with real booking/payment/progress history, not just the brand-new ones.
        clientRepository.findByUserEmail("citva").ifPresent(clients::add);
        return clients;
    }

    private Client createClient(String email) {
        User user = createActivatedUser(email, Role.CLIENT);
        Client client = Client.builder()
                .user(user)
                .payments(new ArrayList<>())
                .clientSessionTrackings(new HashSet<>())
                .clientAppointments(new HashSet<>())
                .build();
        return clientRepository.save(client);
    }

    private User createActivatedUser(String email, Role role) {
        User user = User.builder()
                .email(email)
                .password(passwordEncoder.encode(DEV_PASSWORD))
                .isActivated(true)
                .notificationPreference(NotificationPreference.EMAIL)
                .userRoles(new HashSet<>())
                .build();

        UserRole userRole = new UserRole();
        userRole.setUser(user);
        userRole.setRole(role);
        user.getUserRoles().add(userRole);

        return userRepository.save(user);
    }

    // ---------------------------------------------------------------------------------- payments

    private void seedPayments(List<Client> clients, Session individual, Session smallGroup, Session bigGroup,
                               Map<Integer, Map<Integer, int[]>> tracking) {
        for (Client client : clients) {
            payFor(client, individual, 10 + random.nextInt(8),
                    LocalDate.now().minusMonths(3).plusDays(random.nextInt(15)), tracking);
            payFor(client, individual, 6 + random.nextInt(6),
                    LocalDate.now().minusDays(5 + random.nextInt(10)), tracking);
            payFor(client, smallGroup, 8 + random.nextInt(6),
                    LocalDate.now().minusMonths(2).plusDays(random.nextInt(15)), tracking);
            if (random.nextBoolean()) {
                payFor(client, bigGroup, 10 + random.nextInt(10),
                        LocalDate.now().minusMonths(4).plusDays(random.nextInt(20)), tracking);
            }
        }
    }

    private void payFor(Client client, Session session, int paidAppointments, LocalDate paymentDate,
                         Map<Integer, Map<Integer, int[]>> tracking) {
        paymentRepository.save(Payment.builder()
                .client(client)
                .session(session)
                .paidAppointments(paidAppointments)
                .paymentDate(paymentDate)
                .build());

        tracking.computeIfAbsent(client.getId(), k -> new HashMap<>())
                .computeIfAbsent(session.getId(), k -> new int[]{0, 0})[0] += paidAppointments;
    }

    // ------------------------------------------------------------------------------ appointments

    private void seedAppointments(List<Trainer> trainers, List<Client> clients, Session individual,
                                   Session smallGroup, Session bigGroup, List<Room> rooms,
                                   Map<Integer, Map<Integer, int[]>> tracking) {
        LocalDate today = LocalDate.now();
        DayOfWeek[] pastDays = {DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY};
        DayOfWeek[] futureDays = {DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY};

        int slot = 0;
        for (int w = PAST_WEEKS; w >= 1; w--) {
            for (DayOfWeek day : pastDays) {
                LocalDate date = today.minusWeeks(w).with(ChronoField.DAY_OF_WEEK, day.getValue());
                slot++;
                // ~15% of past slots come out empty - the closest this schema can represent a
                // cancelled/no-show slot, since appointments have no persisted status column
                // (see AGENTS.md "Upgrade: Faza 7 decisions" for why this is a deliberate
                // approximation, not a bug).
                createAppointmentSlot(date, LocalTime.of(8, 0), LocalTime.of(9, 0), individual,
                        trainers, clients, rooms, tracking, slot, pastCount(1), 0.9);
                createAppointmentSlot(date, LocalTime.of(10, 0), LocalTime.of(11, 0), smallGroup,
                        trainers, clients, rooms, tracking, slot, pastCount(1 + random.nextInt(3)), 0.9);
                createAppointmentSlot(date, LocalTime.of(18, 0), LocalTime.of(19, 0), bigGroup,
                        trainers, clients, rooms, tracking, slot, pastCount(3 + random.nextInt(6)), 0.9);
            }
        }

        for (int w = 1; w <= FUTURE_WEEKS; w++) {
            for (DayOfWeek day : futureDays) {
                LocalDate date = today.plusWeeks(w).with(ChronoField.DAY_OF_WEEK, day.getValue());
                slot++;
                // Only ~55% pre-assigned a trainer, so the trainer "without-trainer" self-assign
                // screen and the client "available" list both have real, non-trivial data.
                createAppointmentSlot(date, LocalTime.of(8, 0), LocalTime.of(9, 0), individual,
                        trainers, clients, rooms, tracking, slot, random.nextInt(2), 0.55);
                createAppointmentSlot(date, LocalTime.of(10, 0), LocalTime.of(11, 0), smallGroup,
                        trainers, clients, rooms, tracking, slot, random.nextInt(3), 0.55);
                createAppointmentSlot(date, LocalTime.of(18, 0), LocalTime.of(19, 0), bigGroup,
                        trainers, clients, rooms, tracking, slot, random.nextInt(6), 0.55);
            }
        }
    }

    private int pastCount(int desired) {
        return random.nextInt(100) < 15 ? 0 : desired;
    }

    private void createAppointmentSlot(LocalDate date, LocalTime start, LocalTime end, Session session,
                                        List<Trainer> trainers, List<Client> clients, List<Room> rooms,
                                        Map<Integer, Map<Integer, int[]>> tracking, int slotIndex,
                                        int clientCount, double trainerProbability) {
        Trainer trainer = random.nextDouble() < trainerProbability ? trainers.get(slotIndex % trainers.size()) : null;
        Room room = rooms.isEmpty() || random.nextInt(10) < 4 ? null : rooms.get(slotIndex % rooms.size());

        Appointment appointment = Appointment.builder()
                .date(date)
                .startTime(start)
                .endTime(end)
                .session(session)
                .trainer(trainer)
                .room(room)
                .clientAppointments(new HashSet<>())
                .build();

        int toAdd = Math.min(clientCount, session.getMaxParticipants());
        for (int i = 0; i < toAdd; i++) {
            Client client = clients.get((slotIndex + i) % clients.size());
            if (!tryReserve(tracking, client, session)) continue;
            appointment.getClientAppointments().add(
                    ClientAppointment.builder().client(client).appointment(appointment).build());
        }

        appointmentRepository.save(appointment);
    }

    private boolean tryReserve(Map<Integer, Map<Integer, int[]>> tracking, Client client, Session session) {
        int[] counts = tracking.computeIfAbsent(client.getId(), k -> new HashMap<>())
                .computeIfAbsent(session.getId(), k -> new int[]{0, 0});
        if (counts[0] <= 0) return false;
        counts[0]--;
        counts[1]++;
        return true;
    }

    private void persistSessionTracking(Map<Integer, Map<Integer, int[]>> tracking, List<Client> clients,
                                         List<Session> sessions) {
        Map<Integer, Client> clientsById = clients.stream().collect(Collectors.toMap(Client::getId, c -> c));
        Map<Integer, Session> sessionsById = sessions.stream().collect(Collectors.toMap(Session::getId, s -> s));

        List<ClientSessionTracking> rows = new ArrayList<>();
        tracking.forEach((clientId, perSession) -> perSession.forEach((sessionId, counts) -> {
            Client client = clientsById.get(clientId);
            Session session = sessionsById.get(sessionId);
            // Add on top of whatever a (client, session) row already holds, rather than blindly
            // inserting a fresh one - a dev database that already had real usage before this
            // seeder's marker existed (e.g. earlier phases' manual QA) keeps that history intact.
            ClientSessionTracking row = clientSessionTrackingRepository.findByClientAndSession(client, session)
                    .orElseGet(() -> ClientSessionTracking.builder()
                            .client(client).session(session)
                            .remainingAppointments(0).reservedAppointments(0)
                            .build());
            row.setRemainingAppointments(row.getRemainingAppointments() + counts[0]);
            row.setReservedAppointments(row.getReservedAppointments() + counts[1]);
            rows.add(row);
        }));

        clientSessionTrackingRepository.saveAll(rows);
    }

    // ------------------------------------------------------------------------------- room check-ins

    private void seedRoomCheckIns(List<Client> clients, List<Room> rooms) {
        if (rooms.isEmpty()) return;

        for (int i = 0; i < 15; i++) {
            Client client = clients.get(random.nextInt(clients.size()));
            Room room = rooms.get(random.nextInt(rooms.size()));
            LocalDate date = LocalDate.now().minusDays(1 + random.nextInt(30));
            LocalDateTime checkedIn = date.atTime(7 + random.nextInt(13), random.nextInt(60));
            LocalDateTime checkedOut = checkedIn.plusMinutes(30 + random.nextInt(90));

            roomCheckInRepository.save(RoomCheckIn.builder()
                    .room(room)
                    .client(client)
                    .checkedInAt(checkedIn)
                    .checkedOutAt(checkedOut)
                    .build());
        }

        // One still-open check-in so the live floor plan shows real, non-zero occupancy the
        // instant it's opened, without requiring a manual curl check-in first. Uses the first
        // newly-created client (not the pre-existing "citva") so it doesn't interfere with that
        // account being used for the booking-flow QA in this same phase.
        roomCheckInRepository.save(RoomCheckIn.builder()
                .room(rooms.get(0))
                .client(clients.get(0))
                .checkedInAt(LocalDateTime.now().minusMinutes(20))
                .checkedOutAt(null)
                .build());
    }

    // -------------------------------------------------------------------------------- progress data

    private static final String[] EXERCISES = {"Bench press", "Čučanj", "Mrtvo dizanje", "Plank", "Trčanje 5km"};

    private void seedProgressData(List<Client> clients) {
        for (Client client : clients) {
            double baseWeight = 65 + random.nextInt(40);
            double baseFat = 18 + random.nextInt(12);

            LocalDate entryDate = LocalDate.now().minusMonths(6);
            for (int i = 0; i < 7 && !entryDate.isAfter(LocalDate.now()); i++) {
                double weight = baseWeight - i * (0.3 + random.nextDouble() * 0.4);
                double fat = baseFat - i * (0.2 + random.nextDouble() * 0.3);

                clientProgressEntryRepository.save(ClientProgressEntry.builder()
                        .client(client)
                        .entryDate(entryDate)
                        .weightKg(bd(weight))
                        .bodyFatPercent(bd(fat))
                        .waistCm(bd(80 - i * 0.5 + random.nextDouble()))
                        .chestCm(bd(95 + random.nextDouble() * 2))
                        .hipCm(bd(98 - i * 0.3 + random.nextDouble()))
                        .thighCm(bd(55 + random.nextDouble()))
                        .armCm(bd(32 + random.nextDouble()))
                        .notes(i == 0 ? "Početno merenje" : null)
                        .build());

                entryDate = entryDate.plusWeeks(3);
            }

            LocalDate recordDate = LocalDate.now().minusMonths(4);
            for (int i = 0; i < 3 && !recordDate.isAfter(LocalDate.now()); i++) {
                String exercise = EXERCISES[random.nextInt(EXERCISES.length)];
                RecordUnit unit = exercise.equals("Plank")
                        ? RecordUnit.SECONDS
                        : exercise.startsWith("Trčanje") ? RecordUnit.MINUTES : RecordUnit.KG;
                double value = switch (unit) {
                    case SECONDS -> 40 + i * 15;
                    case MINUTES -> 32 - i * 2;
                    default -> 40 + i * 10 + random.nextInt(20);
                };

                clientPersonalRecordRepository.save(ClientPersonalRecord.builder()
                        .client(client)
                        .exerciseName(exercise)
                        .value(bd(value))
                        .unit(unit)
                        .recordDate(recordDate)
                        .build());

                recordDate = recordDate.plusMonths(1);
            }
        }
    }

    private BigDecimal bd(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }
}
