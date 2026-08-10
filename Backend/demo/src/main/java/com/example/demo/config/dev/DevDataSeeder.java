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
import com.example.demo.repository.user.ClientAppointmentRepository;
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

    private final java.util.Random random = new java.util.Random(42); // fixed seed: same-shaped dataset every fresh run

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.findByEmail(MARKER_EMAIL).isPresent()) {
            log.info("🌱 Dev data already seeded (marker trainer {} exists) - skipping.", MARKER_EMAIL);
            return;
        }

        log.info("🌱 Seeding realistic dev data (manager, trainers, clients, appointments, payments, check-ins, progress)...");

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

        seedAppointmentsForCurrentMonth(trainers, clients, individual, smallGroup, bigGroup, rooms, bookedCounts);
        seedPayments(clients, individual, smallGroup, bigGroup, bookedCounts);

        seedRoomCheckIns(clients, rooms);
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

    // ------------------------------------------------------------------------------------ manager

    /** One ordinary MANAGER (no ADMIN role) - a real example of the non-super-admin manager
     * account introduced alongside Role.ADMIN (see AGENTS.md "Upgrade: manager-hierarchy
     * decisions"). Only the pre-existing "admin" account (V1.0020 migration) gets ADMIN. */
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
        // Same reasoning as including "ogi" above: the pre-existing dev CLIENT account should
        // also show up with real booking/payment/progress history, not just the brand-new ones.
        clientRepository.findByUserEmail("citva").ifPresent(clients::add);

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

    private void seedAppointmentsForCurrentMonth(List<Trainer> trainers, List<Client> clients, Session individual,
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
            int slotCounter = 0;
            int trainerCounter = 0;

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
                    LocalTime time = slots.get(slotCounter % slots.size());
                    slotCounter++;
                    Trainer trainer = workingTrainers.get(trainerCounter % workingTrainers.size());
                    trainerCounter++;
                    Room room = rooms.isEmpty() ? null : rooms.get(random.nextInt(rooms.size()));

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
        }

        // Appointments first - ClientAppointment rows reference their generated ids via FK.
        appointmentRepository.saveAll(appointmentsToSave);
        clientAppointmentRepository.saveAll(clientAppointmentsToSave);
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
                    payments.add(Payment.builder()
                            .client(client)
                            .session(session)
                            .paidAppointments(paid)
                            .paymentDate(LocalDate.now().minusDays(5 + random.nextInt(50)))
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
