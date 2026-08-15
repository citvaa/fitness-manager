package com.example.demo.config.dev;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Component
@Profile("dev")
@RequiredArgsConstructor
@Slf4j
public class DemoDataSeeder implements ApplicationRunner {
    public static final String MARKER_EMAIL = "marko.trener@momentum.demo";
    public static final String DEMO_PASSWORD = "Demo123!";

    private final JdbcTemplate jdbc;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Integer markerCount = jdbc.queryForObject("SELECT COUNT(*) FROM \"user\" WHERE email = ?", Integer.class, MARKER_EMAIL);
        if (markerCount != null && markerCount > 0) {
            log.info("✅ Demo podaci već postoje; relativni seeder je preskočen.");
            return;
        }
        log.info("🔥 Kreiranje realističnog Momentum Fitness demo skupa...");
        seedGymHoursAndHoliday();
        seedTrainers();
        seedClients();
        List<Integer> trainers = jdbc.queryForList("SELECT id FROM trainer ORDER BY id", Integer.class);
        List<Integer> clients = jdbc.queryForList("SELECT id FROM client ORDER BY id", Integer.class);
        seedOperationalHistory(trainers, clients);
        log.info("✅ Demo seeder završen: {} trenera, {} klijenata i relativna istorija/ponuda termina.", trainers.size(), clients.size());
    }

    @Transactional
    public void reseed() {
        List<String> tables = jdbc.queryForList("SELECT tablename FROM pg_tables WHERE schemaname='public' AND tablename <> 'flyway_schema_history' ORDER BY tablename", String.class);
        String quotedTables = tables.stream().map(name -> '"' + name.replace("\"", "\"\"") + '"').reduce((left, right) -> left + "," + right)
                .orElseThrow(() -> new IllegalStateException("No application tables found for reseed"));
        // One TRUNCATE acquires the table locks as a set; the former per-table loop
        // could deadlock with the minute occupancy reader halfway through the wipe.
        jdbc.execute("TRUNCATE TABLE " + quotedTables + " RESTART IDENTITY CASCADE");
        seedFoundations();
        seedGymHoursAndHoliday();
        seedTrainers();
        seedClients();
        List<Integer> trainers = jdbc.queryForList("SELECT id FROM trainer ORDER BY id", Integer.class);
        List<Integer> clients = jdbc.queryForList("SELECT id FROM client ORDER BY id", Integer.class);
        seedOperationalHistory(trainers, clients);
        log.info("✅ Dev baza je potpuno ponovo posejana.");
    }

    private void seedFoundations() {
        for (String role : List.of("MANAGER", "TRAINER", "CLIENT", "ADMIN")) jdbc.update("INSERT INTO role(name) VALUES (?)", role);
        jdbc.update("INSERT INTO session(type,max_participants) VALUES ('INDIVIDUAL',1),('GROUP',3),('GROUP',10)");
        int admin = insertUser("admin@momentum.rs", "MANAGER"); jdbc.update("INSERT INTO user_role(user_id,role) VALUES (?, 'ADMIN')", admin);
        int trainerUser = insertUser("ogi@momentum.rs", "TRAINER"); jdbc.update("INSERT INTO trainer(user_id,employment_date,birth_year,status) VALUES (?,?,?,?)", trainerUser, Date.valueOf(LocalDate.now().minusYears(2)), 1992, "FULL_TIME");
        int clientUser = insertUser("citva@momentum.rs", "CLIENT"); jdbc.update("INSERT INTO client(user_id) VALUES (?)", clientUser);
        Integer gym = jdbc.queryForObject("INSERT INTO gym(name,address,phone,email,brand_color,timezone) VALUES (?,?,?,?,?,?) RETURNING id", Integer.class,
                "Momentum Fitness", "Bulevar oslobođenja 88, Novi Sad", "+381 21 555 018", "zdravo@momentum.rs", "#BAF252", "Europe/Belgrade");
        Object[][] rooms = {{"Kardio panorama","CARDIO",18,55d,55d,370d,205d},{"Zona snage","WEIGHTS",22,455d,55d,485d,205d},{"Pulse studio","GROUP_STUDIO",16,55d,295d,285d,260d},{"Funkcionalna arena","FUNCTIONAL",20,370d,295d,360d,260d},{"Boks studio","GROUP_STUDIO",12,760d,295d,180d,260d}};
        for (Object[] room : rooms) jdbc.update("INSERT INTO room(gym_id,name,type,capacity,pos_x,pos_y,width,height,rotation_degrees) VALUES (?,?,?,?,?,?,?,?,0)",
                gym, room[0], room[1], room[2], room[3], room[4], room[5], room[6]);
    }

    private void seedGymHoursAndHoliday() {
        for (DayOfWeek day : DayOfWeek.values()) {
            boolean weekday = day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY;
            LocalTime opening = weekday ? LocalTime.of(6, 0) : LocalTime.of(8, 0);
            LocalTime closing = weekday ? LocalTime.of(23, 0) : LocalTime.of(20, 0);
            jdbc.update("INSERT INTO gym_schedule(day, opening_time, closing_time) VALUES (?, ?, ?) ON CONFLICT(day) DO UPDATE SET opening_time=EXCLUDED.opening_time, closing_time=EXCLUDED.closing_time", day.name(), opening, closing);
        }
        LocalDate holiday = LocalDate.now().plusMonths(1).withDayOfMonth(1);
        jdbc.update("INSERT INTO holiday(date, description) VALUES (?, ?) ON CONFLICT(date) DO NOTHING", Date.valueOf(holiday), "Dan održavanja i servis opreme");
        jdbc.update("INSERT INTO holiday(date, description) VALUES (?, ?) ON CONFLICT(date) DO NOTHING", Date.valueOf(holiday.plusMonths(1).plusDays(10)), "Praznični neradni dan");
    }

    private List<Integer> seedTrainers() {
        String[][] people = {{MARKER_EMAIL, "1991", "FULL_TIME", "6"}, {"ana.petrovic@momentum.demo", "1994", "FULL_TIME", "3"}, {"nikola.jovanovic@momentum.demo", "1988", "CONTRACT", "5"}, {"milica.stojanovic@momentum.demo", "1997", "CONTRACT", "2"}};
        List<Integer> ids = new ArrayList<>();
        for (int i = 0; i < people.length; i++) {
            int userId = insertUser(people[i][0], "TRAINER");
            Integer trainerId = jdbc.queryForObject("INSERT INTO trainer(user_id, employment_date, birth_year, status) VALUES (?, ?, ?, ?) RETURNING id", Integer.class, userId, Date.valueOf(LocalDate.now().minusYears(Integer.parseInt(people[i][3])).minusMonths(i * 2L)), Integer.valueOf(people[i][1]), people[i][2]);
            ids.add(trainerId);
        }
        return ids;
    }

    private List<Integer> seedClients() {
        String[] firstNames = {"jelena", "luka", "mina", "stefan", "iva", "milica", "nikola", "ana", "marko", "tamara", "nemanja", "sara", "dusan", "teodora", "vuk"};
        String[] lastNames = {"jovanovic", "petrovic", "nikolic", "stojanovic", "ilic", "pavlovic", "markovic", "djurdjevic", "kovacevic", "popovic"};
        List<Integer> ids = new ArrayList<>();
        for (int i = 0; i < 49; i++) {
            String email = firstNames[i % firstNames.length] + "." + lastNames[(i * 3 + i / firstNames.length) % lastNames.length]
                    + (i >= firstNames.length ? i / firstNames.length + 1 : "") + "@clan.momentum.demo";
            int userId = insertUser(email, "CLIENT");
            ids.add(jdbc.queryForObject("INSERT INTO client(user_id) VALUES (?) RETURNING id", Integer.class, userId));
        }
        return ids;
    }

    private int insertUser(String email, String role) {
        Integer userId = jdbc.queryForObject("INSERT INTO \"user\"(email,password,notification_preference,is_activated) VALUES (?,?,?,true) RETURNING id", Integer.class, email, passwordEncoder.encode(DEMO_PASSWORD), "PUSH");
        jdbc.update("INSERT INTO user_role(user_id, role) VALUES (?, ?)", userId, role);
        return userId;
    }

    private void seedOperationalHistory(List<Integer> trainers, List<Integer> clients) {
        List<Integer> sessions = jdbc.queryForList("SELECT id FROM session ORDER BY id", Integer.class);
        List<Integer> rooms = jdbc.queryForList("SELECT id FROM room WHERE type <> 'LOCKER_ROOM' ORDER BY id", Integer.class);
        if (rooms.isEmpty()) throw new IllegalStateException("Demo floor-plan rooms must exist before DemoDataSeeder runs");

        for (int clientIndex = 0; clientIndex < clients.size(); clientIndex++) {
            seedProgress(clients.get(clientIndex), clientIndex);
        }

        Random random = new Random(20260815L);
        Map<Integer, Map<LocalDate, LocalTime[]>> workingRanges = new LinkedHashMap<>();
        LocalDate date = LocalDate.now().withDayOfMonth(1);
        LocalDate monthEnd = date.withDayOfMonth(date.lengthOfMonth());
        int sequence = 0;
        while (!date.isAfter(monthEnd)) {
            List<LocalTime> slots = switch (date.getDayOfWeek()) {
                case SATURDAY -> List.of(LocalTime.of(8, 30), LocalTime.of(10, 30), LocalTime.of(13, 0), LocalTime.of(17, 0));
                case SUNDAY -> List.of(LocalTime.of(9, 0), LocalTime.of(11, 0), LocalTime.of(17, 30));
                default -> List.of(LocalTime.of(7, 0), LocalTime.of(9, 0), LocalTime.of(17, 0), LocalTime.of(18, 30), LocalTime.of(20, 0));
            };
            for (LocalTime start : slots) {
                Integer trainer = sequence % 4 == 0 ? null : trainers.get(sequence % trainers.size());
                int roll = random.nextInt(100);
                int session = roll < 15 ? sessions.get(0) : roll < 50 ? sessions.get(1) : sessions.get(2);
                int room = rooms.get(sequence % rooms.size());
                Integer appointment = jdbc.queryForObject("INSERT INTO appointment(date,start_time,end_time,session_id,trainer_id,room_id) VALUES (?,?,?,?,?,?) RETURNING id", Integer.class, Date.valueOf(date), start, start.plusHours(1), session, trainer, room);
                int capacity = session == sessions.get(0) ? 1 : session == sessions.get(1) ? 3 : 10;
                int participants = session == sessions.get(0) ? 1 : Math.max(2, capacity - random.nextInt(Math.max(1, capacity / 2)));
                for (int p = 0; p < participants; p++) {
                    jdbc.update("INSERT INTO client_appointment(client_id,appointment_id) VALUES (?,?)", clients.get((sequence * 3 + p) % clients.size()), appointment);
                }
                if (trainer != null) {
                    LocalTime[] range = workingRanges.computeIfAbsent(trainer, ignored -> new LinkedHashMap<>())
                            .computeIfAbsent(date, ignored -> new LocalTime[]{start, start.plusHours(1)});
                    if (start.isBefore(range[0])) range[0] = start;
                    if (start.plusHours(1).isAfter(range[1])) range[1] = start.plusHours(1);
                }
                sequence++;
            }
            date = date.plusDays(1);
        }

        workingRanges.forEach((trainer, dates) -> dates.forEach((workingDate, range) ->
                jdbc.update("INSERT INTO trainer_schedule(trainer_id,date,start_time,end_time,status) VALUES (?,?,?,?,?)",
                        trainer, Date.valueOf(workingDate), range[0], range[1], "WORKING")));

        seedPaymentsAndTracking(clients, sessions);

        for (int i = 0; i < 24; i++) {
            LocalDateTime in = LocalDate.now().minusDays(3 + i * 2L).atTime(17 + i % 3, 0);
            jdbc.update("INSERT INTO room_check_in(room_id,client_id,checked_in_at,checked_out_at) VALUES (?,?,?,?)", rooms.get(i % rooms.size()), clients.get(i % clients.size()), Timestamp.valueOf(in), Timestamp.valueOf(in.plusMinutes(55 + i % 20)));
        }
    }

    private void seedPaymentsAndTracking(List<Integer> clients, List<Integer> sessions) {
        for (int clientIndex = 0; clientIndex < clients.size(); clientIndex++) {
            int client = clients.get(clientIndex);
            for (int session : sessions) {
                Integer booked = jdbc.queryForObject("SELECT COUNT(*) FROM client_appointment ca JOIN appointment a ON a.id=ca.appointment_id WHERE ca.client_id=? AND a.session_id=?", Integer.class, client, session);
                Integer reserved = jdbc.queryForObject("SELECT COUNT(*) FROM client_appointment ca JOIN appointment a ON a.id=ca.appointment_id WHERE ca.client_id=? AND a.session_id=? AND a.date>=CURRENT_DATE", Integer.class, client, session);
                int paid = clientIndex % 10 == 0 ? Math.max(0, booked - 2) : booked;
                if (booked > 0) {
                    jdbc.update("INSERT INTO payment(client_id,session_id,paid_appointments,payment_date) VALUES (?,?,?,?)", client, session, paid, Date.valueOf(LocalDate.now().minusDays(5 + clientIndex % 20)));
                    jdbc.update("INSERT INTO client_session_tracking(client_id,session_id,remaining_appointments,reserved_appointments) VALUES (?,?,?,?)", client, session, Math.max(0, paid - reserved), reserved);
                }
            }
        }
    }

    private void seedProgress(int client, int index) {
        BigDecimal base = BigDecimal.valueOf(72 + index % 12);
        for (int point = 0; point < 7; point++) {
            jdbc.update("INSERT INTO client_progress_entry(client_id,entry_date,weight_kg,body_fat_percent,waist_cm,chest_cm,hip_cm,thigh_cm,arm_cm,notes) VALUES (?,?,?,?,?,?,?,?,?,?)", client, Date.valueOf(LocalDate.now().minusDays(168 - point * 28L)), base.subtract(BigDecimal.valueOf(point * .45)), BigDecimal.valueOf(25 - point * .45 - index % 4), BigDecimal.valueOf(91 - point * .7 - index % 3), BigDecimal.valueOf(98 + point * .25 + index % 5), BigDecimal.valueOf(101 - point * .35 + index % 4), BigDecimal.valueOf(57 - point * .15 + index % 3), BigDecimal.valueOf(31 + point * .2 + index % 4), point == 6 ? "Stabilan napredak i dobra energija." : "Redovno mesečno merenje.");
        }
        String exercise = switch (index % 3) { case 0 -> "Čučanj"; case 1 -> "Mrtvo dizanje"; default -> "Bench press"; };
        for (int point = 0; point < 3; point++) {
            jdbc.update("INSERT INTO client_personal_record(client_id,exercise_name,value,unit,record_date) VALUES (?,?,?,?,?)", client, exercise, BigDecimal.valueOf(55 + index % 15 * 2L + point * 5L), "KG", Date.valueOf(LocalDate.now().minusMonths(2L - point)));
        }
    }
}
