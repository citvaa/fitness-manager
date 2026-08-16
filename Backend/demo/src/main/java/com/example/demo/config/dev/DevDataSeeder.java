package com.example.demo.config.dev;

import com.example.demo.enums.EmploymentStatus;
import com.example.demo.enums.NotificationPreference;
import com.example.demo.enums.RecordUnit;
import com.example.demo.enums.Role;
import com.example.demo.enums.RoomType;
import com.example.demo.enums.SessionType;
import com.example.demo.enums.WorkStatus;
import com.example.demo.model.Appointment;
import com.example.demo.model.Holiday;
import com.example.demo.model.Payment;
import com.example.demo.model.Session;
import com.example.demo.model.gym.Gym;
import com.example.demo.model.gym.Room;
import com.example.demo.model.gym.RoomCheckIn;
import com.example.demo.model.progress.ClientPersonalRecord;
import com.example.demo.model.progress.ClientProgressEntry;
import com.example.demo.model.schedule.GymSchedule;
import com.example.demo.model.schedule.TrainerSchedule;
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
import com.example.demo.repository.gym.GymRepository;
import com.example.demo.repository.gym.RoomCheckInRepository;
import com.example.demo.repository.gym.RoomRepository;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Populates a fresh `dev` database with a dataset that looks like a gym that has actually been
 * running for months, rather than the handful of test rows the Flyway dev-data migrations add
 * (see AGENTS.md "Upgrade: Faza 7 decisions" / "Upgrade: manager-testing round 2 decisions" for
 * the full rationale). Deliberately a Java {@link CommandLineRunner}, not a Flyway migration: the
 * whole point is data expressed relative to "now" (the current calendar month's appointments),
 * which a static SQL migration timestamped at write-time cannot express correctly on every future
 * run.
 *
 * <p>Idempotency: guarded by checking whether {@link #MARKER_EMAIL} (the first trainer this
 * seeder creates) already exists - if so, the whole method is skipped. This mirrors the
 * `WHERE NOT EXISTS` guards the Flyway dev-data migrations use for the same reason (a manager
 * account seeded by an earlier migration, or manual testing, must not be duplicated on restart).
 *
 * <p><b>Appointment/payment scheme (round 2):</b> appointments are generated for every day of the
 * <i>current calendar month</i> (both the days already elapsed and the days still to come), not a
 * fixed past/future window - see AGENTS.md for why. Each of the 5 trainers works a fixed set of 3
 * weekdays for the whole month; each of the ~50 clients independently prefers 3 weekdays of their
 * own. On any date where at least one trainer is working, every client who prefers that weekday is
 * booked into an appointment on that date (individual/small-group/big-group, weighted so most
 * bookings are group sessions - that's what lets 50 clients fit into a handful of trainers'
 * schedules while each client still gets their own ~3 sessions/week). Payments are generated
 * afterwards from the actual resulting booking counts per (client, session type): ~90% of clients
 * get a payment that fully covers what they booked, the rest are deliberately left with fewer paid
 * appointments than booked (see AGENTS.md "Known issues" - reservations have no floor check against
 * remaining balance, so this "used more than paid for" state is a realistic outcome, not a seeding
 * bug).
 */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class DevDataSeeder implements CommandLineRunner {

    private static final String DEV_PASSWORD = "password123"; // same known dev password as V1.0017
    private static final String MARKER_EMAIL = "marko.markovic@fitpro.dev";
    private static final int NEW_GENERATED_CLIENT_COUNT = 44; // + 5 explicit + "citva" = ~50 total

    private final UserRepository userRepository;
    private final TrainerRepository trainerRepository;
    private final ClientRepository clientRepository;
    private final PasswordEncoder passwordEncoder;
    private final GymScheduleRepository gymScheduleRepository;
    private final HolidayRepository holidayRepository;
    private final SessionRepository sessionRepository;
    private final AppointmentRepository appointmentRepository;
    private final ClientAppointmentRepository clientAppointmentRepository;
    private final ClientSessionTrackingRepository clientSessionTrackingRepository;
    private final PaymentRepository paymentRepository;
    private final RoomRepository roomRepository;
    private final RoomCheckInRepository roomCheckInRepository;
    private final ClientProgressEntryRepository clientProgressEntryRepository;
    private final ClientPersonalRecordRepository clientPersonalRecordRepository;
    private final GymRepository gymRepository;
    private final UserRoleRepository userRoleRepository;
    private final TrainerScheduleRepository trainerScheduleRepository;

    private final java.util.Random random = new java.util.Random(42); // fixed seed: same-shaped dataset every fresh run

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.findByEmail(MARKER_EMAIL).isPresent()) {
            log.info("🌱 Dev data already seeded (marker trainer {} exists) - skipping.", MARKER_EMAIL);
            return;
        }
        seedAll();
    }

    /**
     * Wipes every dev/test row this seeder owns and reseeds from scratch, without needing a
     * container restart or a Docker volume wipe - see AGENTS.md "Upgrade: dev-data ownership
     * decisions" for why this exists and how it's exposed ({@code POST /api/dev/reseed}, wired up
     * in {@code DevDataController}). Safe to call at any time on the {@code dev} profile: schema
     * (Flyway) is untouched, only the rows this class is responsible for are deleted and rebuilt.
     */
    @Transactional
    public void reseed() {
        log.info("♻️ Reseeding dev data: wiping existing dev/test rows and rebuilding from scratch...");
        wipeAllDevData();
        seedAll();
        log.info("✅ Reseed complete.");
    }

    /**
     * Deletes every row this seeder is responsible for, in FK-safe (children-before-parents)
     * order - see AGENTS.md for the full dependency reasoning. Deliberately does NOT touch
     * {@code Session} rows (seeded by a base Flyway migration, referenced by surviving
     * Appointment/Payment/ClientSessionTracking FKs conceptually but not actually populated once
     * this runs first) or the {@code role} reference table.
     */
    private void wipeAllDevData() {
        clientPersonalRecordRepository.deleteAllInBatch();
        clientProgressEntryRepository.deleteAllInBatch();
        roomCheckInRepository.deleteAllInBatch();
        clientAppointmentRepository.deleteAllInBatch();
        appointmentRepository.deleteAllInBatch();
        paymentRepository.deleteAllInBatch();
        clientSessionTrackingRepository.deleteAllInBatch();
        trainerScheduleRepository.deleteAllInBatch();
        clientRepository.deleteAllInBatch();
        trainerRepository.deleteAllInBatch();
        userRoleRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
        roomRepository.deleteAllInBatch();
        gymRepository.deleteAllInBatch();
        gymScheduleRepository.deleteAllInBatch();
        holidayRepository.deleteAllInBatch();
    }

    /** The actual seed logic, shared by {@link #run} (first-boot, guarded by the marker check)
     * and {@link #reseed} (unconditional, called right after {@link #wipeAllDevData}). This
     * method alone is now the single source of truth for what a "freshly seeded" dev database
     * looks like - including what used to be the Flyway dev-data migrations' job (Gym/Room
     * creation, the admin/ogi/citva accounts, admin's ADMIN role, room sizes). Those migrations
     * are left in place (never edited/deleted - Flyway checksums are locked) since they still run
     * on a truly fresh Postgres volume, but every value they insert is now redundant with - and,
     * on a reseed, actually replaced by - what this method builds. */
    private void seedAll() {
        log.info("🌱 Seeding realistic dev data (gym, admin, manager, trainers, clients, appointments, payments, check-ins, progress)...");

        ensureGymAndRooms();
        ensureAdminUser();
        ensureGymSchedule();
        ensureHolidays();

        seedManager();
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

        // clientId -> sessionId -> count of appointments actually booked this run, tallied while
        // generating appointments below and used afterwards to generate matching payments/
        // tracking rows (see class Javadoc "Appointment/payment scheme").
        Map<Integer, Map<Integer, Integer>> bookedCounts = new HashMap<>();

        GeneratedAppointments generated = seedAppointmentsForCurrentMonth(
                trainers, clients, individual, smallGroup, bigGroup, rooms, bookedCounts);
        seedPayments(clients, individual, smallGroup, bigGroup, bookedCounts);

        seedRoomCheckIns(generated, clients, rooms);
        seedProgressData(clients);

        log.info("✅ Dev data seeded: 1 manager, {} trainers, {} clients, appointments across the current calendar month.",
                trainers.size(), clients.size());
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

    // --------------------------------------------------------------------------- gym / rooms / admin

    /** Creates the single Gym row and its 5 rooms if none exist yet - this used to be the Flyway
     * dev-data migration V1.0016's job (see AGENTS.md "Upgrade: dev-data ownership decisions").
     * Guarded by "does a Gym already exist" rather than a marker check, since a fresh Postgres
     * volume still runs that migration first (never edited/deleted), so this only actually
     * inserts rows itself after a {@link #reseed()} wipe. */
    private void ensureGymAndRooms() {
        Gym gym = gymRepository.findAll().stream().findFirst().orElseGet(() -> gymRepository.save(Gym.builder()
                .name("FitPro Gym")
                .address("Bulevar oslobođenja 12, Novi Sad")
                .contactEmail("info@fitpro.rs")
                .contactPhone("+381601234567")
                .primaryColor("#2f83fb")
                .timezone("Europe/Belgrade")
                .build()));

        if (!roomRepository.findByGymId(gym.getId()).isEmpty()) return;

        roomRepository.saveAll(List.of(
                room(gym, "Sala za tegove", RoomType.WORKOUT_FLOOR, 25, 0.0, 0.0, 12.0, 10.0, "#2f83fb"),
                room(gym, "Kardio zona", RoomType.WORKOUT_FLOOR, 20, 13.0, 0.0, 10.0, 10.0, "#0ea5e9"),
                room(gym, "Joga studio", RoomType.STUDIO, 15, 0.0, 11.0, 8.0, 6.0, "#a855f7"),
                // Was "Svlačionica" (LOCKER_ROOM) - a locker room can't realistically host a
                // training session, but seedAppointmentsForCurrentMonth() picks a room from ALL 5
                // rooms with no type filter (see AGENTS.md "Upgrade: training-room seed decisions"
                // for why the fix is replacing the room, not adding a filter), so appointments were
                // randomly landing there. Replaced with a boxing/martial-arts studio - same 11-
                // character name length as "Svlačionica" ("Boks studio"), so the existing 7.5x6
                // footprint (already tuned to exactly the minimum for an 11-char name, see the
                // room-minimum-size upgrade below) stays valid without any resize.
                room(gym, "Boks studio", RoomType.STUDIO, 16, 9.0, 11.0, 7.5, 6.0, "#64748b"),
                // Was "Recepcija" (RECEPTION) - same problem as above. Replaced with a functional/
                // TRX-training room; "TRX sala" (8 characters) needs only 6.0 width units at
                // RoomSizingPolicy's minimum, well under the existing 8.0x6 footprint, so no resize
                // was needed here either (kept as-is per the "existing dims as a starting point"
                // instruction - just double-checked, not re-tuned).
                room(gym, "TRX sala", RoomType.WORKOUT_FLOOR, 12, 17.5, 11.0, 8.0, 6.0, "#f59e0b")));
    }

    private Room room(Gym gym, String name, RoomType type, int capacity,
                       double posX, double posY, double width, double height, String color) {
        return Room.builder().gym(gym).name(name).type(type).capacity(capacity)
                .posX(posX).posY(posY).width(width).height(height).rotationDegrees(0.0).color(color).build();
    }

    /** Creates the "admin" account (MANAGER + ADMIN roles) if it doesn't already exist - this
     * used to be split across migrations V1.0002/V1.0003/V1.0017/V1.0020 (see AGENTS.md "Upgrade:
     * dev-data ownership decisions"). Those migrations still create/activate it on a truly fresh
     * Postgres volume; this only actually runs after a {@link #reseed()} wipe. */
    private void ensureAdminUser() {
        if (userRepository.findByEmail("admin").isPresent()) return;

        User admin = User.builder()
                .email("admin")
                .password(passwordEncoder.encode(DEV_PASSWORD))
                .isActivated(true)
                .notificationPreference(NotificationPreference.PUSH)
                .userRoles(new HashSet<>())
                .build();
        admin = userRepository.save(admin);

        // Two roles on one user hits the same BaseEntity id-less equals()/hashCode() landmine as
        // ClientAppointment (see class Javadoc "Appointment/payment scheme") if both UserRole
        // instances are added to a Set before either is persisted - save each individually via
        // its own repository instead, same workaround pattern used elsewhere in this class.
        userRoleRepository.save(newUserRole(admin, Role.MANAGER));
        userRoleRepository.save(newUserRole(admin, Role.ADMIN));
    }

    private UserRole newUserRole(User user, Role role) {
        UserRole userRole = new UserRole();
        userRole.setUser(user);
        userRole.setRole(role);
        return userRole;
    }

    // ------------------------------------------------------------------------------------ manager

    /** One ordinary MANAGER (no ADMIN role) - a real example of the non-super-admin manager
     * account introduced alongside Role.ADMIN (see AGENTS.md "Upgrade: manager-hierarchy
     * decisions"). Only "admin" (see {@link #ensureAdminUser()}) gets ADMIN. */
    private void seedManager() {
        createActivatedUser("milan.milic@fitpro.dev", Role.MANAGER);
    }

    // ---------------------------------------------------------------------------- trainers/clients

    private List<Trainer> seedTrainers() {
        List<Trainer> trainers = new ArrayList<>();
        trainers.add(createTrainer(MARKER_EMAIL, LocalDate.now().minusYears(3), 1988, EmploymentStatus.FULL_TIME));
        trainers.add(createTrainer("jelena.jovanovic@fitpro.dev", LocalDate.now().minusYears(1).minusMonths(4), 1993, EmploymentStatus.FULL_TIME));
        trainers.add(createTrainer("nikola.nikolic@fitpro.dev", LocalDate.now().minusMonths(8), 1996, EmploymentStatus.CONTRACT));
        trainers.add(createTrainer("dragan.dragic@fitpro.dev", LocalDate.now().minusYears(2), 1990, EmploymentStatus.FULL_TIME));
        // Include "ogi" too, so the seeded appointments give it (the account every earlier
        // phase's docs/screenshots reference) real data as well - find-or-create since this
        // seeder now owns that account (see ensureAdminUser()/AGENTS.md "Upgrade: dev-data
        // ownership decisions"); migration V1.0009 still creates it on a truly fresh volume, but
        // a reseed() wipe removes it and this recreates it identically.
        trainers.add(trainerRepository.findByUserEmail("ogi")
                .orElseGet(() -> createTrainer("ogi", LocalDate.now().minusYears(3), 1990, EmploymentStatus.FULL_TIME)));
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

    // ASCII-transliterated Serbian names (matches the existing "ana.petrovic@fitpro.dev" style -
    // no diacritics in email local parts). 30 first names x 20 last names = 600 distinct
    // combinations, comfortably more than NEW_GENERATED_CLIENT_COUNT, so a simple modulo pairing
    // over a run of consecutive indices never repeats a combination.
    private static final String[] FIRST_NAMES = {
            "marko", "ana", "jovan", "milica", "nikola", "jelena", "petar", "ivana", "stefan", "teodora",
            "aleksandar", "katarina", "nemanja", "marija", "vladimir", "sara", "dusan", "tamara", "filip", "milena",
            "bojan", "natasa", "igor", "sanja", "zoran", "ljiljana", "goran", "snezana", "vladan", "tijana",
    };
    private static final String[] LAST_NAMES = {
            "petrovic", "jovanovic", "nikolic", "ilic", "pavlovic", "stojanovic", "markovic", "djordjevic", "popovic", "ristic",
            "simic", "kostic", "radovic", "milic", "stankovic", "tomic", "jankovic", "antic", "vukovic", "lazic",
    };

    private List<Client> seedClients() {
        List<Client> clients = new ArrayList<>();
        Set<String> usedEmails = new HashSet<>();

        String[] explicitEmails = {
                "ana.petrovic@fitpro.dev", "stefan.stojanovic@fitpro.dev", "milica.ilic@fitpro.dev",
                "petar.pavlovic@fitpro.dev", "jovana.jovanovic@fitpro.dev",
        };
        for (String email : explicitEmails) {
            clients.add(createClient(email));
            usedEmails.add(email);
        }
        // Same reasoning/find-or-create pattern as "ogi" above: "citva" should also show up with
        // real booking/payment/progress history, not just the brand-new clients.
        clients.add(clientRepository.findByUserEmail("citva").orElseGet(() -> createClient("citva")));

        // i%30/i%20 modulo pairing can coincidentally reproduce one of the explicit emails above
        // (e.g. i=3 -> "milica.ilic@fitpro.dev") - skip forward past any such collision rather
        // than fail on the resulting duplicate-email constraint violation.
        int added = 0;
        for (int i = 0; added < NEW_GENERATED_CLIENT_COUNT; i++) {
            String first = FIRST_NAMES[i % FIRST_NAMES.length];
            String last = LAST_NAMES[i % LAST_NAMES.length];
            String email = first + "." + last + "@fitpro.dev";
            if (!usedEmails.add(email)) continue;
            clients.add(createClient(email));
            added++;
        }
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

    // ------------------------------------------------------------------------------ appointments

    // Trainers each work a fixed 3 weekdays for the whole month - chosen so every weekday has at
    // least one, and most have two, trainers available (needed to spread ~50 clients' bookings
    // across the month without any single trainer being double- or triple-booked at once).
    private static final DayOfWeek[][] TRAINER_WORKDAY_SETS = {
            {DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY},
            {DayOfWeek.TUESDAY, DayOfWeek.THURSDAY, DayOfWeek.SATURDAY},
            {DayOfWeek.MONDAY, DayOfWeek.THURSDAY, DayOfWeek.SATURDAY},
            {DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY},
            {DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY, DayOfWeek.SUNDAY},
    };

    /** Time slots available per weekday, kept inside that day's gym hours (see
     * ensureGymSchedule) - Sunday closes at 15:00 and Saturday at 20:00, both narrower than the
     * 06:00-22:00 weekday window. */
    private List<LocalTime> slotsFor(DayOfWeek day) {
        if (day == DayOfWeek.SUNDAY) {
            return List.of(LocalTime.of(9, 0), LocalTime.of(11, 0), LocalTime.of(13, 0));
        }
        if (day == DayOfWeek.SATURDAY) {
            return List.of(LocalTime.of(8, 0), LocalTime.of(10, 0), LocalTime.of(12, 0), LocalTime.of(16, 0), LocalTime.of(18, 0));
        }
        return List.of(LocalTime.of(8, 0), LocalTime.of(10, 0), LocalTime.of(12, 0), LocalTime.of(16, 0), LocalTime.of(18, 0), LocalTime.of(19, 30));
    }

    /** Just enough of the generated data for seedRoomCheckIns() to auto-check-in clients into the
     * rooms they were actually booked into, instead of picking a completely unrelated room/
     * client/date combination - see AGENTS.md "Upgrade: seeder AI-insights data decisions". */
    private record GeneratedAppointments(List<Appointment> appointments, List<ClientAppointment> clientAppointments) {
    }

    private GeneratedAppointments seedAppointmentsForCurrentMonth(List<Trainer> trainers, List<Client> clients, Session individual,
                                                  Session smallGroup, Session bigGroup, List<Room> rooms,
                                                  Map<Integer, Map<Integer, Integer>> bookedCounts) {
        LocalDate first = LocalDate.now().withDayOfMonth(1);
        LocalDate last = first.withDayOfMonth(first.lengthOfMonth());

        // Each trainer keeps its fixed weekday set for the whole month (assigned by index, wrapping
        // if there are more trainers than pattern rows).
        Map<Integer, Set<DayOfWeek>> trainerWorkdays = new HashMap<>();
        for (int i = 0; i < trainers.size(); i++) {
            trainerWorkdays.put(trainers.get(i).getId(),
                    EnumSet.copyOf(List.of(TRAINER_WORKDAY_SETS[i % TRAINER_WORKDAY_SETS.length])));
        }

        // Each client independently prefers 3 distinct weekdays of their own for the whole month -
        // this is what makes "~3 sessions/week per client" hold across the month regardless of how
        // many weeks it has.
        Map<Integer, Set<DayOfWeek>> clientPreferredDays = new HashMap<>();
        DayOfWeek[] allDays = DayOfWeek.values();
        for (Client client : clients) {
            Set<DayOfWeek> preferred = new HashSet<>();
            while (preferred.size() < 3) {
                preferred.add(allDays[random.nextInt(allDays.length)]);
            }
            clientPreferredDays.put(client.getId(), preferred);
        }

        List<Appointment> appointmentsToSave = new ArrayList<>();
        List<ClientAppointment> clientAppointmentsToSave = new ArrayList<>();
        // One WORKING TrainerSchedule row per (trainer, date) actually booked below - see
        // "Upgrade: dev-seeder trainer-schedule gap fix" in AGENTS.md. Without this, every
        // seeded appointment looks "uncovered" against the real TrainerSchedule table (empty
        // after seeding) even though the seeder only ever assigns a trainer to a date within
        // their own fixed TRAINER_WORKDAY_SETS pattern - a seeder gap, not a validation gap
        // (AppointmentServiceImpl.validateTrainerWorkingSchedule already requires real coverage
        // for appointments created through the actual API).
        List<TrainerSchedule> trainerScheduleToSave = new ArrayList<>();
        // Participant count per in-progress appointment, tracked here rather than via
        // Appointment.getClientAppointments().size() - BaseEntity's Lombok @Data equals()/
        // hashCode() only compares audit fields (never the subclass id, see AGENTS.md "Known
        // issues"), so every freshly-built, unsaved ClientAppointment compares equal to every
        // other one and a HashSet silently drops all but the first. An IdentityHashMap keyed by
        // the Appointment instance (identity, not equals()) sidesteps that entirely, and
        // ClientAppointment rows are saved directly via their own repository below instead of
        // through Appointment's Set field.
        Map<Appointment, Integer> participantCount = new java.util.IdentityHashMap<>();

        for (LocalDate date = first; !date.isAfter(last); date = date.plusDays(1)) {
            DayOfWeek dow = date.getDayOfWeek();
            // Never generate an appointment on a date the real validation
            // (AppointmentServiceImpl#validateNotHoliday/#validateGymSchedule) would itself
            // reject - a holiday, or a weekday with no GymSchedule row at all. In practice
            // ensureHolidays()/ensureGymSchedule() never overlap the current-month generation
            // window today (holidays sit 2 months back / next Jan 1, and every weekday has a
            // schedule row), but this seeder writes directly via the repository, bypassing
            // AppointmentServiceImpl.create()'s validation entirely - this guard keeps that true
            // even if either of those changes later. See AGENTS.md "Upgrade: seeder AI-insights
            // data decisions".
            if (holidayRepository.existsByDate(date) || !gymScheduleRepository.existsByDay(dow)) {
                continue;
            }
            List<Trainer> workingTrainers = trainers.stream()
                    .filter(t -> trainerWorkdays.get(t.getId()).contains(dow))
                    .toList();
            if (workingTrainers.isEmpty()) continue;

            List<Client> interestedClients = clients.stream()
                    .filter(c -> clientPreferredDays.get(c.getId()).contains(dow))
                    .toList();
            if (interestedClients.isEmpty()) continue;

            List<LocalTime> slots = slotsFor(dow);
            // Appointments still open to more participants for this date, per session type.
            List<Appointment> openIndividual = new ArrayList<>();
            List<Appointment> openSmallGroup = new ArrayList<>();
            List<Appointment> openBigGroup = new ArrayList<>();

            // (trainer, time) and (room, time) pairs already claimed by an appointment created
            // for THIS date - shared across all three session types below (an individual and a
            // group appointment for the same trainer at the same time are just as much a double
            // booking as two of the same type). All appointments in this seeder are exactly one
            // hour and every slot in slotsFor() is spaced so distinct slots never overlap in
            // time, so "same trainer/room + same slot" is the only overlap case that can occur
            // here - a same-key check is therefore sufficient, no interval-overlap math needed.
            Set<String> occupiedTrainerSlots = new HashSet<>();
            Map<LocalTime, Set<Integer>> occupiedRoomsAtSlot = new HashMap<>();
            int comboCounter = 0;
            int totalCombos = slots.size() * workingTrainers.size();
            int skippedForNoFreeSlot = 0;
            // Trainers actually assigned at least one appointment on this date - the exact set
            // that needs a matching WORKING TrainerSchedule row below, not the whole
            // workingTrainers list (a trainer eligible to work this weekday may still end up with
            // zero appointments on a specific date if there aren't enough clients/combos).
            Set<Trainer> bookedTrainersToday = new HashSet<>();

            for (Client client : interestedClients) {
                // Weighted towards group sessions - the only way ~50 clients' bookings fit into a
                // handful of trainers' daily schedules while still leaving room for individual
                // sessions to exist at all (see class Javadoc).
                int roll = random.nextInt(100);
                Session sessionType = roll < 15 ? individual : roll < 45 ? smallGroup : bigGroup;
                List<Appointment> openList = sessionType == individual ? openIndividual
                        : sessionType == smallGroup ? openSmallGroup : openBigGroup;

                Appointment appointment = openList.isEmpty() ? null : openList.get(openList.size() - 1);
                if (appointment == null) {
                    // Walk the (slot, trainer) combo space deterministically from comboCounter
                    // until an unclaimed pair is found, instead of rotating two independent
                    // counters that can revisit an already-used pair (the original bug: with
                    // independent trainerCounter/slotCounter, the same (trainer, time) pair
                    // recurs every lcm(workingTrainers.size(), slots.size()) new appointments).
                    LocalTime time = null;
                    Trainer trainer = null;
                    for (int attempt = 0; attempt < totalCombos; attempt++) {
                        int combo = comboCounter % totalCombos;
                        comboCounter++;
                        LocalTime candidateTime = slots.get(combo % slots.size());
                        Trainer candidateTrainer = workingTrainers.get(combo / slots.size());
                        if (occupiedTrainerSlots.add(candidateTrainer.getId() + "@" + candidateTime)) {
                            time = candidateTime;
                            trainer = candidateTrainer;
                            break;
                        }
                    }
                    if (trainer == null) {
                        // Every (trainer, slot) combo for this date is already claimed - a real,
                        // if rare, scheduling ceiling, not a bug. Skip this client for this date
                        // rather than double-book a trainer.
                        skippedForNoFreeSlot++;
                        continue;
                    }
                    bookedTrainersToday.add(trainer);

                    Room room = null;
                    if (!rooms.isEmpty()) {
                        Set<Integer> usedRoomsAtSlot = occupiedRoomsAtSlot.computeIfAbsent(time, t -> new HashSet<>());
                        int roomStart = random.nextInt(rooms.size());
                        for (int i = 0; i < rooms.size(); i++) {
                            Room candidate = rooms.get((roomStart + i) % rooms.size());
                            if (usedRoomsAtSlot.add(candidate.getId())) {
                                room = candidate;
                                break;
                            }
                        }
                        // If every room is already claimed at this exact slot, room is left null
                        // (a nullable column - see AGENTS.md) rather than double-booking one.
                    }

                    appointment = Appointment.builder()
                            .date(date)
                            .startTime(time)
                            .endTime(time.plusHours(1))
                            .session(sessionType)
                            .trainer(trainer)
                            .room(room)
                            .clientAppointments(new HashSet<>())
                            .build();
                    openList.add(appointment);
                    appointmentsToSave.add(appointment);
                    participantCount.put(appointment, 0);
                }

                clientAppointmentsToSave.add(
                        ClientAppointment.builder().client(client).appointment(appointment).build());
                int newCount = participantCount.merge(appointment, 1, Integer::sum);
                if (newCount >= sessionType.getMaxParticipants()) {
                    // Identity-based removal (not List.remove(Object), which is equals()-based
                    // and hits the same BaseEntity landmine described above).
                    Appointment fullAppointment = appointment;
                    openList.removeIf(a -> a == fullAppointment);
                }

                bookedCounts.computeIfAbsent(client.getId(), k -> new HashMap<>())
                        .merge(sessionType.getId(), 1, Integer::sum);
            }

            if (skippedForNoFreeSlot > 0) {
                log.info("🌱 {} client(s) not booked on {} - every trainer/time-slot combination ({} total) was already taken.",
                        skippedForNoFreeSlot, date, totalCombos);
            }

            // One WORKING shift per trainer actually booked today, spanning this weekday's whole
            // slotsFor() range (simpler and more realistic than a tight per-trainer min/max of only
            // their own assigned slots, and still always a superset of what needs to be covered -
            // see AGENTS.md "Upgrade: dev-seeder trainer-schedule gap fix").
            if (!bookedTrainersToday.isEmpty()) {
                LocalTime dayStart = slots.get(0);
                LocalTime dayEnd = slots.get(slots.size() - 1).plusHours(1);
                for (Trainer trainer : bookedTrainersToday) {
                    trainerScheduleToSave.add(TrainerSchedule.builder()
                            .trainer(trainer)
                            .date(date)
                            .startTime(dayStart)
                            .endTime(dayEnd)
                            .status(WorkStatus.WORKING)
                            .build());
                }
            }

            // A handful of trainer-less "open slots" - the whole point of the TRAINER self-assign
            // marketplace (POST /api/appointment/{id}/assign, already fully wired since Faza 7)
            // is meaningless without any actual trainer-less appointment to assign to, and
            // create()/createRecurringWeekly() reject request.trainerId == null before this round
            // (see AGENTS.md "Upgrade: trainer self-assign decisions"). ~1 in 4 dates gets one,
            // reusing occupiedRoomsAtSlot so it can never double-book a room that a real
            // trainer-led appointment above already claimed at that exact slot on this date - no
            // trainer/occupiedTrainerSlots check needed since there is no trainer to conflict.
            if (!rooms.isEmpty() && random.nextInt(100) < 25) {
                LocalTime openSlot = slots.get(random.nextInt(slots.size()));
                Set<Integer> usedRoomsAtOpenSlot = occupiedRoomsAtSlot.computeIfAbsent(openSlot, t -> new HashSet<>());
                int roomStart = random.nextInt(rooms.size());
                for (int i = 0; i < rooms.size(); i++) {
                    Room candidate = rooms.get((roomStart + i) % rooms.size());
                    if (usedRoomsAtOpenSlot.add(candidate.getId())) {
                        appointmentsToSave.add(Appointment.builder()
                                .date(date)
                                .startTime(openSlot)
                                .endTime(openSlot.plusHours(1))
                                .session(individual)
                                .trainer(null)
                                .room(candidate)
                                .clientAppointments(new HashSet<>())
                                .build());
                        break;
                    }
                }
            }
        }

        // Appointments first - ClientAppointment rows reference their generated ids via FK.
        appointmentRepository.saveAll(appointmentsToSave);
        clientAppointmentRepository.saveAll(clientAppointmentsToSave);
        trainerScheduleRepository.saveAll(trainerScheduleToSave);

        return new GeneratedAppointments(appointmentsToSave, clientAppointmentsToSave);
    }

    // ---------------------------------------------------------------------------------- payments

    /** Builds payments (and the matching ClientSessionTracking rows) from the appointment counts
     * actually booked above - see class Javadoc "Appointment/payment scheme". */
    private void seedPayments(List<Client> clients, Session individual, Session smallGroup, Session bigGroup,
                               Map<Integer, Map<Integer, Integer>> bookedCounts) {
        Map<Integer, Session> sessionsById = Map.of(
                individual.getId(), individual,
                smallGroup.getId(), smallGroup,
                bigGroup.getId(), bigGroup);

        List<Payment> payments = new ArrayList<>();
        List<ClientSessionTracking> trackingRows = new ArrayList<>();

        for (Client client : clients) {
            Map<Integer, Integer> perSession = bookedCounts.get(client.getId());
            if (perSession == null || perSession.isEmpty()) continue;

            // ~90% of clients paid for (at least) everything they booked; the rest are a
            // deliberate "used more than paid for" edge case (see class Javadoc).
            boolean fullyPaid = random.nextInt(100) < 90;

            for (Map.Entry<Integer, Integer> entry : perSession.entrySet()) {
                Session session = sessionsById.get(entry.getKey());
                int booked = entry.getValue();
                int paid = fullyPaid ? booked : Math.max(0, booked - (1 + random.nextInt(3)));

                if (paid > 0) {
                    // Within the last 30 days (0-29 days back) - matches
                    // ManagerInsightsServiceImpl's own 30-day window exactly, so the manager-
                    // insights dashboard's "paid appointments per session type" aggregation
                    // actually sees these payments instead of roughly half of them landing
                    // outside the window it queries. See AGENTS.md "Upgrade: seeder AI-insights
                    // data decisions".
                    payments.add(Payment.builder()
                            .client(client)
                            .session(session)
                            .paidAppointments(paid)
                            .paymentDate(LocalDate.now().minusDays(random.nextInt(30)))
                            .build());
                }

                ClientSessionTracking row = clientSessionTrackingRepository.findByClientAndSession(client, session)
                        .orElseGet(() -> ClientSessionTracking.builder()
                                .client(client).session(session)
                                .remainingAppointments(0).reservedAppointments(0)
                                .build());
                // remaining can end up negative for the underpaid clients above - a realistic
                // outcome, not a seeding bug (see AGENTS.md "Known issues": reservations have no
                // floor check against remaining balance).
                row.setRemainingAppointments(row.getRemainingAppointments() + paid - booked);
                row.setReservedAppointments(row.getReservedAppointments() + booked);
                trackingRows.add(row);
            }
        }

        paymentRepository.saveAll(payments);
        clientSessionTrackingRepository.saveAll(trackingRows);
    }

    // ------------------------------------------------------------------------------- room check-ins

    /** Auto-check-ins clients into the room they were actually booked into via a real generated
     * appointment (rather than a completely independent random room/client/date pick, the
     * previous behavior) - see AGENTS.md "Upgrade: seeder AI-insights data decisions" for why: it
     * gives every room a check-in count that traces back to real occupancy, at a volume that
     * scales with how many appointments were actually generated rather than a flat 15. */
    private void seedRoomCheckIns(GeneratedAppointments generated, List<Client> clients, List<Room> rooms) {
        if (rooms.isEmpty()) return;

        // Appointment's own clientAppointments Set is deliberately left empty by the generator
        // above (see its own comment on the BaseEntity id-less equals()/hashCode() bug) - group
        // the flat ClientAppointment list back by appointment *identity* (not equals()) instead.
        Map<Appointment, List<Client>> clientsByAppointment = new java.util.IdentityHashMap<>();
        for (ClientAppointment ca : generated.clientAppointments()) {
            clientsByAppointment.computeIfAbsent(ca.getAppointment(), a -> new ArrayList<>()).add(ca.getClient());
        }

        LocalDateTime now = LocalDateTime.now();
        List<RoomCheckIn> checkIns = new ArrayList<>();

        for (Appointment appointment : generated.appointments()) {
            if (appointment.getRoom() == null) continue;
            LocalDateTime end = LocalDateTime.of(appointment.getDate(), appointment.getEndTime());
            if (end.isAfter(now)) continue; // hasn't happened yet - can't have been checked into

            List<Client> booked = clientsByAppointment.get(appointment);
            if (booked == null || booked.isEmpty()) continue;

            // Not every past appointment has check-in records (a manager/trainer may simply not
            // have bothered), and not every booked client who attended necessarily gets checked
            // in either - keeps this looking like real, imperfect manual data entry rather than
            // a suspiciously exact 1:1 mirror of the booking table.
            if (random.nextInt(100) >= 65) continue;

            LocalDateTime start = LocalDateTime.of(appointment.getDate(), appointment.getStartTime());
            for (Client client : booked) {
                if (random.nextInt(100) >= 70) continue;
                LocalDateTime checkedIn = start.plusMinutes(random.nextInt(10));
                LocalDateTime checkedOut = end.minusMinutes(random.nextInt(10));
                if (!checkedOut.isAfter(checkedIn)) {
                    checkedOut = checkedIn.plusMinutes(30);
                }
                checkIns.add(RoomCheckIn.builder()
                        .room(appointment.getRoom())
                        .client(client)
                        .checkedInAt(checkedIn)
                        .checkedOutAt(checkedOut)
                        .build());
            }
        }

        // A smaller top-up independent of any booked appointment (walk-in gym floor usage isn't
        // exclusively tied to a session) - kept small since the bulk of volume now comes from
        // real appointments above, unlike the previous flat-15-total approach.
        for (int i = 0; i < 10; i++) {
            Client client = clients.get(random.nextInt(clients.size()));
            Room room = rooms.get(random.nextInt(rooms.size()));
            LocalDate date = LocalDate.now().minusDays(1 + random.nextInt(29));
            LocalDateTime checkedIn = date.atTime(7 + random.nextInt(13), random.nextInt(60));
            LocalDateTime checkedOut = checkedIn.plusMinutes(30 + random.nextInt(90));
            checkIns.add(RoomCheckIn.builder()
                    .room(room)
                    .client(client)
                    .checkedInAt(checkedIn)
                    .checkedOutAt(checkedOut)
                    .build());
        }

        roomCheckInRepository.saveAll(checkIns);

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
