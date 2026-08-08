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
import java.util.List;

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
        List<Integer> trainers = seedTrainers();
        List<Integer> clients = seedClients();
        seedTrainerSchedules(trainers);
        seedOperationalHistory(trainers, clients);
        log.info("✅ Demo seeder završen: {} trenera, {} klijenata i relativna istorija/ponuda termina.", trainers.size(), clients.size());
    }

    private void seedGymHoursAndHoliday() {
        for (DayOfWeek day : DayOfWeek.values()) {
            LocalTime opening = day == DayOfWeek.SUNDAY ? LocalTime.of(8, 0) : LocalTime.of(6, 0);
            LocalTime closing = day == DayOfWeek.SUNDAY ? LocalTime.of(20, 0) : LocalTime.of(23, 0);
            jdbc.update("INSERT INTO gym_schedule(day, opening_time, closing_time) VALUES (?, ?, ?) ON CONFLICT(day) DO UPDATE SET opening_time=EXCLUDED.opening_time, closing_time=EXCLUDED.closing_time", day.name(), opening, closing);
        }
        LocalDate holiday = LocalDate.now().plusMonths(1).withDayOfMonth(1);
        jdbc.update("INSERT INTO holiday(date, description) VALUES (?, ?) ON CONFLICT(date) DO NOTHING", Date.valueOf(holiday), "Dan održavanja i servis opreme");
    }

    private List<Integer> seedTrainers() {
        String[][] people = {{MARKER_EMAIL, "1991", "FULL_TIME"}, {"ana.trener@momentum.demo", "1994", "FULL_TIME"}, {"nikola.trener@momentum.demo", "1988", "CONTRACT"}};
        List<Integer> ids = new ArrayList<>();
        for (int i = 0; i < people.length; i++) {
            int userId = insertUser(people[i][0], "TRAINER");
            Integer trainerId = jdbc.queryForObject("INSERT INTO trainer(user_id, employment_date, birth_year, status) VALUES (?, ?, ?, ?) RETURNING id", Integer.class, userId, Date.valueOf(LocalDate.now().minusYears(4 - i)), Integer.valueOf(people[i][1]), people[i][2]);
            ids.add(trainerId);
        }
        return ids;
    }

    private List<Integer> seedClients() {
        String[] emails = {"jelena.klijent@momentum.demo", "luka.klijent@momentum.demo", "mina.klijent@momentum.demo", "stefan.klijent@momentum.demo", "iva.klijent@momentum.demo"};
        List<Integer> ids = new ArrayList<>();
        for (String email : emails) {
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

    private void seedTrainerSchedules(List<Integer> trainers) {
        for (int offset = -56; offset <= 35; offset++) {
            LocalDate date = LocalDate.now().plusDays(offset);
            if (date.getDayOfWeek() == DayOfWeek.SUNDAY) continue;
            for (int trainer : trainers) jdbc.update("INSERT INTO trainer_schedule(trainer_id,date,start_time,end_time,status) VALUES (?,?,?,?,?)", trainer, Date.valueOf(date), LocalTime.of(7, 0), LocalTime.of(21, 0), "WORKING");
        }
    }

    private void seedOperationalHistory(List<Integer> trainers, List<Integer> clients) {
        List<Integer> sessions = jdbc.queryForList("SELECT id FROM session ORDER BY id", Integer.class);
        List<Integer> rooms = jdbc.queryForList("SELECT id FROM room WHERE type <> 'LOCKER_ROOM' ORDER BY id", Integer.class);
        if (rooms.isEmpty()) throw new IllegalStateException("Demo floor-plan rooms must exist before DemoDataSeeder runs");

        for (int clientIndex = 0; clientIndex < clients.size(); clientIndex++) {
            int client = clients.get(clientIndex);
            for (int session : sessions) {
                jdbc.update("INSERT INTO payment(client_id,session_id,paid_appointments,payment_date) VALUES (?,?,?,?)", client, session, session == sessions.get(0) ? 12 : 20, Date.valueOf(LocalDate.now().minusDays(70 - clientIndex * 3)));
                jdbc.update("INSERT INTO client_session_tracking(client_id,session_id,remaining_appointments,reserved_appointments) VALUES (?,?,?,?)", client, session, 8, 4);
            }
            seedProgress(client, clientIndex);
        }

        int sequence = 0;
        for (int offset = -56; offset <= 28; offset++) {
            LocalDate date = LocalDate.now().plusDays(offset);
            if (date.getDayOfWeek() == DayOfWeek.SUNDAY) continue;
            for (int slot = 0; slot < 2; slot++) {
                LocalTime start = LocalTime.of(slot == 0 ? 9 : 18, (sequence % 2) * 30);
                Integer trainer = offset > 0 && sequence % 4 == 0 ? null : trainers.get(sequence % trainers.size());
                int session = sessions.get(sequence % sessions.size());
                int room = rooms.get(sequence % rooms.size());
                Integer appointment = jdbc.queryForObject("INSERT INTO appointment(date,start_time,end_time,session_id,trainer_id,room_id) VALUES (?,?,?,?,?,?) RETURNING id", Integer.class, Date.valueOf(date), start, start.plusHours(1), session, trainer, room);
                if (offset < 0 || (offset > 0 && sequence % 3 == 0)) {
                    int participantLimit = session == sessions.get(0) ? 1 : 2;
                    for (int p = 0; p < participantLimit; p++) jdbc.update("INSERT INTO client_appointment(client_id,appointment_id) VALUES (?,?)", clients.get((sequence + p) % clients.size()), appointment);
                }
                sequence++;
            }
        }

        for (int i = 0; i < 24; i++) {
            LocalDateTime in = LocalDate.now().minusDays(3 + i * 2L).atTime(17 + i % 3, 0);
            jdbc.update("INSERT INTO room_check_in(room_id,client_id,checked_in_at,checked_out_at) VALUES (?,?,?,?)", rooms.get(i % rooms.size()), clients.get(i % clients.size()), Timestamp.valueOf(in), Timestamp.valueOf(in.plusMinutes(55 + i % 20)));
        }
    }

    private void seedProgress(int client, int index) {
        BigDecimal base = BigDecimal.valueOf(88 - index * 4L);
        for (int point = 0; point < 7; point++) {
            jdbc.update("INSERT INTO client_progress_entry(client_id,entry_date,weight_kg,body_fat_percent,waist_cm,chest_cm,hip_cm,thigh_cm,arm_cm,notes) VALUES (?,?,?,?,?,?,?,?,?,?)", client, Date.valueOf(LocalDate.now().minusDays(84 - point * 14L)), base.subtract(BigDecimal.valueOf(point * .8)), BigDecimal.valueOf(27 - point * .7 - index * .4), BigDecimal.valueOf(96 - point - index), BigDecimal.valueOf(101 + point * .3), BigDecimal.valueOf(104 - point * .6), BigDecimal.valueOf(59 - point * .2), BigDecimal.valueOf(32 + point * .25), point == 6 ? "Stabilan napredak i dobra energija." : "Redovno merenje u dvonedeljnom ciklusu.");
        }
        jdbc.update("INSERT INTO client_personal_record(client_id,exercise_name,value,unit,record_date) VALUES (?,?,?,?,?)", client, "Čučanj", BigDecimal.valueOf(70 + index * 5L), "KG", Date.valueOf(LocalDate.now().minusDays(18)));
        jdbc.update("INSERT INTO client_personal_record(client_id,exercise_name,value,unit,record_date) VALUES (?,?,?,?,?)", client, "Plank", BigDecimal.valueOf(90 + index * 12L), "SECONDS", Date.valueOf(LocalDate.now().minusDays(9)));
        jdbc.update("INSERT INTO client_personal_record(client_id,exercise_name,value,unit,record_date) VALUES (?,?,?,?,?)", client, "Trčanje 5 km", BigDecimal.valueOf(28 - index), "MINUTES", Date.valueOf(LocalDate.now().minusDays(4)));
    }
}
