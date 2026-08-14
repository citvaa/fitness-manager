package com.example.demo.service.impl;

import com.example.demo.dto.AppointmentDTO;
import com.example.demo.dto.gym.RoomDTO;
import com.example.demo.dto.user.TrainerDTO;
import com.example.demo.enums.WorkStatus;
import com.example.demo.mapper.AppointmentMapper;
import com.example.demo.mapper.gym.RoomMapper;
import com.example.demo.mapper.user.TrainerMapper;
import com.example.demo.model.*;
import com.example.demo.model.gym.Room;
import com.example.demo.model.schedule.GymSchedule;
import com.example.demo.model.schedule.TrainerSchedule;
import com.example.demo.model.user.Client;
import com.example.demo.model.user.ClientAppointment;
import com.example.demo.model.user.ClientSessionTracking;
import com.example.demo.model.user.Trainer;
import com.example.demo.repository.*;
import com.example.demo.repository.gym.RoomRepository;
import com.example.demo.repository.schedule.GymScheduleRepository;
import com.example.demo.repository.schedule.TrainerScheduleRepository;
import com.example.demo.repository.user.ClientAppointmentRepository;
import com.example.demo.repository.user.ClientRepository;
import com.example.demo.repository.user.ClientSessionTrackingRepository;
import com.example.demo.repository.user.TrainerRepository;
import com.example.demo.service.AppointmentService;
import com.example.demo.service.HolidayService;
import com.example.demo.service.notification.NotificationService;
import com.example.demo.service.params.request.appointment.CreateAppointmentRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AppointmentServiceImpl implements AppointmentService {

    /** How many weekly occurrences a "fixed weekly appointment" generates ahead of its first date
     * - see AGENTS.md "Upgrade: fixed weekly appointment decisions" for why 8 (~2 months) was
     * chosen over "rest of this month" (too short near month-end) or unbounded (would let one
     * click silently create months/years of rows). The manager can create the same recurring slot
     * again later to extend it. */
    private static final int RECURRING_WEEKS_AHEAD = 8;

    private final SessionRepository sessionRepository;
    private final TrainerRepository trainerRepository;
    private final ClientRepository clientRepository;
    private final AppointmentRepository appointmentRepository;
    private final AppointmentMapper appointmentMapper;
    private final GymScheduleRepository gymScheduleRepository;
    private final TrainerScheduleRepository trainerScheduleRepository;
    private final ClientSessionTrackingRepository clientSessionTrackingRepository;
    private final NotificationService notificationService;
    private final ClientAppointmentRepository clientAppointmentRepository;
    private final RoomRepository roomRepository;
    private final HolidayService holidayService;
    private final TrainerMapper trainerMapper;
    private final RoomMapper roomMapper;

    @Transactional
    public AppointmentDTO create(@NotNull CreateAppointmentRequest request) throws JsonProcessingException {
        validateAppointment(request);

        Session session = fetchSession(request.getSessionId());
        Trainer trainer = fetchTrainer(request.getTrainerId());
        Room room = fetchRoom(request.getRoomId());

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
            notificationService.sendTrainerAssignmentNotification(trainer, appointmentDTO);
        }

        return appointmentDTO;
    }

    @Transactional
    public List<AppointmentDTO> createRecurringWeekly(@NotNull CreateAppointmentRequest request) throws JsonProcessingException {
        List<AppointmentDTO> created = new ArrayList<>();
        // Per-date failure reasons, kept regardless of whether the series ends up succeeding -
        // only surfaced to the caller if the WHOLE series fails (see below). A holiday hit on one
        // of the 8 weekly dates is expected/normal and must not read as a problem when at least
        // one other week succeeded - see AGENTS.md "Upgrade: fixed weekly appointment error
        // reporting decisions".
        List<String> failureReasons = new ArrayList<>();
        LocalDate firstDate = request.getDate();

        for (int week = 0; week < RECURRING_WEEKS_AHEAD; week++) {
            CreateAppointmentRequest occurrence = new CreateAppointmentRequest(
                    firstDate.plusWeeks(week), request.getStartTime(), request.getEndTime(),
                    request.getSessionId(), request.getTrainerId(), request.getRoomId(),
                    request.getClientIds(), false);
            try {
                created.add(create(occurrence));
            } catch (IllegalArgumentException e) {
                // One week's occurrence conflicting (holiday, trainer already booked, gym closed
                // that day, etc.) must not abort the whole series - skip just that occurrence and
                // keep generating the rest. See AGENTS.md "Upgrade: fixed weekly appointment
                // decisions".
                log.warn("⚠️ Skipping recurring occurrence on {}: {}", occurrence.getDate(), e.getMessage());
                failureReasons.add(occurrence.getDate() + ": " + e.getMessage());
            }
        }

        if (created.isEmpty()) {
            throw new IllegalArgumentException("Nijedna instanca fiksnog termina nije mogla biti kreirana. Razlog po datumu:\n" +
                    String.join("\n", failureReasons));
        }

        return created;
    }

    @Transactional
    public AppointmentDTO addTrainer(Integer appointmentId, Integer trainerId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new EntityNotFoundException("Termin nije pronađen"));

        Trainer trainer = trainerRepository.findById(trainerId)
                .orElseThrow(() -> new EntityNotFoundException("Trener nije pronađen"));

        appointment.setTrainer(trainer);

        return appointmentMapper.toDto(appointmentRepository.save(appointment));
    }

    @Transactional
    public AppointmentDTO removeTrainer(Integer id) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Termin nije pronađen"));

        appointment.setTrainer(null);

        return appointmentMapper.toDto(appointmentRepository.save(appointment));
    }

    @Transactional
    public AppointmentDTO addClients(Integer appointmentId, @NotNull Set<Integer> clientIds) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new EntityNotFoundException("Termin nije pronađen"));

        // Filter out clients already on this appointment before doing anything else - otherwise
        // re-adding an already-assigned client would double-charge their session tracking and
        // silently double-count them against capacity below. See AGENTS.md ("Upgrade: Faza 9
        // decisions" - the removeClient/addClients regression found after Faza 9 first gave this
        // method a real UI caller).
        Set<Integer> alreadyAssigned = appointment.getClientAppointments().stream()
                .map(clientAppointment -> clientAppointment.getClient().getId())
                .collect(Collectors.toSet());
        Set<Integer> newClientIds = clientIds.stream()
                .filter(id -> !alreadyAssigned.contains(id))
                .collect(Collectors.toSet());

        int capacity = appointment.getSession().getMaxParticipants();
        int currentCount = appointment.getClientAppointments().size();
        if (currentCount + newClientIds.size() > capacity) {
            throw new IllegalArgumentException("Dodavanje " + newClientIds.size()
                    + " klijenta bi premašilo kapacitet ovog termina od " + capacity
                    + " (trenutno prijavljeno " + currentCount + ")");
        }

        Set<ClientAppointment> clientAppointments = createClientAppointments(newClientIds, appointment);

        appointment.getClientAppointments().addAll(clientAppointments);
        appointmentRepository.save(appointment);

        return appointmentMapper.toDto(appointment);
    }

    @Transactional
    public AppointmentDTO removeClient(Integer appointmentId, Integer clientId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new EntityNotFoundException("Termin nije pronađen"));

        Optional<ClientAppointment> toRemove = appointment.getClientAppointments().stream()
                .filter(clientAppointment -> clientAppointment.getClient().getId().equals(clientId))
                .findFirst();

        // Refund the client's session tracking exactly the way cancel() does - a MANAGER removing
        // a client must not leave their session credit permanently "spent" with no way to get it
        // back. See AGENTS.md ("Upgrade: Faza 9 decisions") - this was a real, previously
        // undiscovered gap, not new behavior.
        if (toRemove.isPresent()) {
            appointment.getClientAppointments().remove(toRemove.get());
            ClientSessionTracking tracking = getOrCreateClientSessionTracking(toRemove.get().getClient(), appointment.getSession());
            decrementReservedAppointments(tracking);
        }

        return appointmentMapper.toDto(appointmentRepository.save(appointment));
    }

    public List<AppointmentDTO> getAll() {
        return appointmentMapper.toDto(appointmentRepository.findAll());
    }

    public List<AppointmentDTO> getAvailable() {
        return appointmentRepository.findAll().stream()
                .filter(appointment -> appointment.getClientAppointments().size() < appointment.getSession().getMaxParticipants())
                .map(appointmentMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public AppointmentDTO reserve(Integer id) {
        Client client = getAuthenticatedClient();

        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Termin nije pronađen!"));

        if (appointment.getClientAppointments().size() >= appointment.getSession().getMaxParticipants()) {
            throw new RuntimeException("Nema slobodnih mesta za ovaj termin!");
        }

        ClientAppointment clientAppointment = createClientAppointment(client, appointment);
        appointment.getClientAppointments().add(clientAppointment);

        AppointmentDTO appointmentDTO = appointmentMapper.toDto(appointmentRepository.save(appointment));
        notificationService.sendManagerAlert("Nova rezervacija: " + client.getUser().getEmail()
                + " je zakazao/la termin " + appointmentDTO.getDate() + " u " + appointmentDTO.getStartTime());

        return appointmentDTO;
    }

    @Transactional
    public AppointmentDTO cancel(Integer id) {
        Client client = getAuthenticatedClient();

        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Termin nije pronađen"));

        LocalDateTime appointmentTime = LocalDateTime.of(appointment.getDate(), appointment.getStartTime());
        LocalDateTime cancellationDeadline = appointmentTime.minusHours(24);

        if (LocalDateTime.now().isBefore(cancellationDeadline)) {
            boolean removed = appointment.getClientAppointments().removeIf(clientAppointment ->
                    clientAppointment.getClient().getId().equals(client.getId()));

            if (!removed) {
                throw new RuntimeException("Klijent nije prijavljen na ovaj termin!");
            }

            ClientSessionTracking tracking = getOrCreateClientSessionTracking(client, appointment.getSession());
            decrementReservedAppointments(tracking);

            return appointmentMapper.toDto(appointmentRepository.save(appointment));
        } else {
            throw new RuntimeException("Prekasno za otkazivanje! Otkazivanje mora biti najmanje 24 sata pre termina.");
        }
    }

    public List<AppointmentDTO> getAllWithoutTrainer() {
        return appointmentRepository.findAll().stream()
                .filter(appointment -> appointment.getTrainer() == null)
                .map(appointmentMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public AppointmentDTO assign(Integer appointmentId) {
        Pair<Trainer, Appointment> trainerAppointment = getAuthenticatedTrainerAndAppointment(appointmentId);
        trainerAppointment.getSecond().setTrainer(trainerAppointment.getFirst());
        AppointmentDTO appointmentDTO = appointmentMapper.toDto(appointmentRepository.save(trainerAppointment.getSecond()));

        Trainer trainer = trainerAppointment.getFirst();
        notificationService.sendManagerAlert("Trener " + trainer.getUser().getEmail()
                + " je preuzeo/la termin bez trenera " + appointmentDTO.getDate() + " u " + appointmentDTO.getStartTime());

        return appointmentDTO;
    }

    @Transactional
    public AppointmentDTO unassign(Integer appointmentId) {
        Pair<Trainer, Appointment> trainerAppointment = getAuthenticatedTrainerAndAppointment(appointmentId);
        if (!trainerAppointment.getFirst().equals(trainerAppointment.getSecond().getTrainer())) {
            throw new RuntimeException("Trener nije dodeljen ovom terminu!");
        }
        trainerAppointment.getSecond().setTrainer(null);
        return appointmentMapper.toDto(appointmentRepository.save(trainerAppointment.getSecond()));
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

    public List<AppointmentDTO> getMyAppointmentsAsClient() {
        Client client = getAuthenticatedClient();
        return appointmentRepository.findByClientAppointmentsClientIdOrderByDateDescStartTimeDesc(client.getId())
                .stream()
                .map(appointmentMapper::toDto)
                .toList();
    }

    public List<AppointmentDTO> getMyAppointmentsAsTrainer() {
        Trainer trainer = getAuthenticatedTrainer();
        return appointmentRepository.findByTrainerIdOrderByDateDescStartTimeDesc(trainer.getId())
                .stream()
                .map(appointmentMapper::toDto)
                .toList();
    }

    public List<TrainerDTO> getAvailableTrainers(LocalDate date, LocalTime startTime, LocalTime endTime) {
        List<Trainer> available = trainerRepository.findAll().stream()
                .filter(t -> isTrainerAvailable(t.getId(), date, startTime, endTime))
                .filter(t -> findTrainerConflict(t.getId(), date, startTime, endTime).isEmpty())
                .toList();
        return trainerMapper.toDto(available);
    }

    public List<RoomDTO> getAvailableRooms(LocalDate date, LocalTime startTime, LocalTime endTime) {
        List<Room> available = roomRepository.findAll().stream()
                .filter(r -> findRoomConflict(r.getId(), date, startTime, endTime).isEmpty())
                .toList();
        return roomMapper.toDto(available);
    }









    private void validateAppointment(@NotNull CreateAppointmentRequest request) {
        // Room stays mandatory (manager-testing round 3) - an unassigned room made occupancy
        // tracking meaningless. Trainer went back to being OPTIONAL in the trainer-self-assign
        // round: the "termin bez trenera" marketplace (assign/unassign, already fully wired since
        // Faza 7) only makes sense if create() can actually produce a trainer-less appointment in
        // the first place - see AGENTS.md "Upgrade: trainer self-assign decisions".
        // validateTrainerWorkingSchedule/validateTrainerNotDoubleBooked below are already
        // null-safe (no-op when trainerId is null), so no other change is needed here.
        if (request.getRoomId() == null) {
            throw new IllegalArgumentException("Soba je obavezna za termin!");
        }
        validateDateAndTimeRange(request.getDate(), request.getStartTime(), request.getEndTime());
        validateNotHoliday(request.getDate());
        validateGymSchedule(request.getDate(), request.getStartTime(), request.getEndTime());
        validateTrainerWorkingSchedule(request.getTrainerId(), request.getDate(), request.getStartTime(), request.getEndTime());
        validateTrainerNotDoubleBooked(request.getTrainerId(), request.getDate(), request.getStartTime(), request.getEndTime());
        validateRoomNotDoubleBooked(request.getRoomId(), request.getDate(), request.getStartTime(), request.getEndTime());
        validateClientAvailability(request.getClientIds(), request.getDate(), request.getStartTime(), request.getEndTime());
    }

    /** Holiday is a reason distinct from "gym closed that day of week" or "trainer/room busy" -
     * checked and reported separately so the caller can tell them apart, rather than a holiday
     * surfacing as a confusing "trainer already busy" message (the trainer simply has no WORKING
     * TrainerSchedule row on a holiday). See AGENTS.md "Upgrade: fixed weekly appointment error
     * reporting decisions". */
    private void validateNotHoliday(@NotNull LocalDate date) {
        if (holidayService.isGymClosedOn(date)) {
            throw new IllegalArgumentException("Teretana je zatvorena " + date + " zbog praznika - termin ne može biti zakazan tog datuma.");
        }
    }

    private void validateDateAndTimeRange(@NotNull LocalDate date, @NotNull LocalTime startTime, @NotNull LocalTime endTime) {
        if (startTime.isAfter(endTime) || startTime.equals(endTime)) {
            throw new IllegalArgumentException("Vreme početka mora biti pre vremena završetka!");
        }

        if (date.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Datum termina mora biti u budućnosti!");
        }
    }

    private void validateGymSchedule(@NotNull LocalDate date, @NotNull LocalTime startTime, LocalTime endTime) {
        GymSchedule gymSchedule = gymScheduleRepository.findByDay(date.getDayOfWeek())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Radno vreme teretane nije definisano za " + date + " (" + date.getDayOfWeek() + ")."));

        if (startTime.isBefore(gymSchedule.getOpeningTime()) || endTime.isAfter(gymSchedule.getClosingTime())) {
            throw new IllegalArgumentException("Termin " + date + " od " + startTime + " do " + endTime +
                    " je van radnog vremena teretane za taj dan (" + gymSchedule.getOpeningTime() +
                    " - " + gymSchedule.getClosingTime() + ").");
        }
    }

    /** Checks the trainer actually has a WORKING TrainerSchedule shift covering this time - a
     * distinct failure reason from "already booked on another appointment" below (an unstaffed
     * trainer isn't "busy", they're simply not scheduled to work then). See AGENTS.md's Known
     * issues entry on mandatory trainer/room for why a seeded trainer with no TrainerSchedule rows
     * hits this. */
    private void validateTrainerWorkingSchedule(Integer id, LocalDate date, LocalTime startTime, LocalTime endTime) {
        if (id != null && !isTrainerAvailable(id, date, startTime, endTime)) {
            throw new IllegalArgumentException("Trener " + trainerLabel(id) + " nema radnu smenu koja pokriva " +
                    date + " od " + startTime + " do " + endTime + " - proverite raspored rada trenera za taj dan.");
        }
    }

    /** Checks the trainer isn't already booked on a different, time-overlapping appointment -
     * previously not checked at all (only working-hours coverage was, above), so two overlapping
     * appointments for the same trainer could both be created as long as one WORKING shift covered
     * both. Reports the exact conflicting appointment's time rather than a generic "already busy". */
    private void validateTrainerNotDoubleBooked(Integer trainerId, LocalDate date, LocalTime startTime, LocalTime endTime) {
        if (trainerId == null) {
            return;
        }
        findTrainerConflict(trainerId, date, startTime, endTime).ifPresent(conflict -> {
            throw new IllegalArgumentException("Trener " + trainerLabel(trainerId) + " je već zauzet " + date + " od " +
                    conflict.getStartTime() + " do " + conflict.getEndTime() + " drugim terminom.");
        });
    }

    /** Rooms had no double-booking check at all before this - only trainers did (and even that
     * only checked working-hours coverage, see above). A room is now treated the same way a
     * trainer is: reject if another appointment overlaps this room/time, naming the exact
     * conflicting appointment's time. */
    private void validateRoomNotDoubleBooked(Integer roomId, LocalDate date, LocalTime startTime, LocalTime endTime) {
        if (roomId == null) {
            return;
        }
        findRoomConflict(roomId, date, startTime, endTime).ifPresent(conflict -> {
            throw new IllegalArgumentException("Soba " + roomLabel(roomId) + " je već zauzeta " + date + " od " +
                    conflict.getStartTime() + " do " + conflict.getEndTime() + " drugim terminom.");
        });
    }

    /** Shared by validateTrainerNotDoubleBooked() above and getAvailableTrainers() below - the one
     * place that knows what "trainer overlap" means, so the picker-filtering endpoint and the
     * create()-time rejection can never disagree. Same LessThanEqual/GreaterThanEqual overlap
     * semantics as the pre-existing client-overlap check (touching boundaries count as a
     * conflict). */
    private Optional<Appointment> findTrainerConflict(Integer trainerId, LocalDate date, LocalTime startTime, LocalTime endTime) {
        return appointmentRepository
                .findByTrainerIdAndDateAndStartTimeLessThanEqualAndEndTimeGreaterThanEqual(trainerId, date, endTime, startTime)
                .stream().findFirst();
    }

    /** Shared by validateRoomNotDoubleBooked() above and getAvailableRooms() below - see
     * findTrainerConflict()'s javadoc for why this is factored out rather than re-checked
     * separately on the frontend. */
    private Optional<Appointment> findRoomConflict(Integer roomId, LocalDate date, LocalTime startTime, LocalTime endTime) {
        return appointmentRepository
                .findByRoomIdAndDateAndStartTimeLessThanEqualAndEndTimeGreaterThanEqual(roomId, date, endTime, startTime)
                .stream().findFirst();
    }

    /** Human-readable trainer identifier for error messages - the trainer's email (User has no
     * name field, see AGENTS.md's domain model section), falling back to a bare "ID X" only if the
     * trainer row itself can't be found (should not happen in practice since trainerId is
     * validated to exist before these checks run, but avoids an NPE if it ever does). */
    private String trainerLabel(Integer id) {
        return trainerRepository.findById(id)
                .map(t -> t.getUser() != null && t.getUser().getEmail() != null ? t.getUser().getEmail() : "ID " + id)
                .orElse("ID " + id);
    }

    /** Human-readable room identifier for error messages - the room's name, same fallback
     * reasoning as trainerLabel() above. */
    private String roomLabel(Integer id) {
        return roomRepository.findById(id).map(Room::getName).orElse("ID " + id);
    }

    private void validateClientAvailability(Set<Integer> ids, LocalDate date, LocalTime startTime, LocalTime endTime) {
        if (ids != null) {
            for (Integer clientId : ids) {
                if (!isClientAvailable(clientId, date, startTime, endTime)) {
                    throw new IllegalArgumentException("Klijent sa ID " + clientId + " je već zakazan u ovom terminu!");
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
                .orElseThrow(() -> new EntityNotFoundException("Sesija nije pronađena"));
    }

    private Trainer fetchTrainer(Integer id) {
        return (id != null) ? trainerRepository.findById(id).orElse(null) : null;
    }

    private Room fetchRoom(Integer id) {
        return (id != null) ? roomRepository.findById(id).orElse(null) : null;
    }

    private Set<ClientAppointment> createClientAppointments(Set<Integer> clientIds, Appointment appointment) {
        if (clientIds == null || clientIds.isEmpty()) {
            return Collections.emptySet();
        }

        // createClientAppointment(client, appointment) below already looks up (or creates) the
        // tracking row and increments it - this method must NOT also increment before delegating
        // to it, or every client passed through create()'s initial clientIds or addClients()
        // gets double-charged (reservedAppointments +2 / remainingAppointments -2 for one booking).
        // A real, previously undiscovered bug found while writing regression tests for the
        // addClients()/removeClient() tracking bugs below - see AGENTS.md
        // ("Upgrade: Faza 9 decisions").
        return clientIds.stream()
                .map(clientId -> {
                    Client client = clientRepository.findById(clientId)
                            .orElseThrow(() -> new EntityNotFoundException("Klijent nije pronađen"));

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
            throw new AccessDeniedException("Neovlašćen pristup!");
        }

        String email = jwt.getClaim("email");

        return clientRepository.findByUserEmail(email)
                .orElseThrow(() -> new RuntimeException("Klijent nije pronađen za prijavljenog korisnika!"));
    }

    private @NotNull Pair<Trainer, Appointment> getAuthenticatedTrainerAndAppointment(Integer id) {
        Trainer trainer = getAuthenticatedTrainer();

        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Termin nije pronađen"));

        return Pair.of(trainer, appointment);
    }

    private Trainer getAuthenticatedTrainer() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new AccessDeniedException("Neovlašćen pristup!");
        }

        String email = jwt.getClaim("email");

        return trainerRepository.findByUserEmail(email)
                .orElseThrow(() -> new RuntimeException("Trener nije pronađen za prijavljenog korisnika!"));
    }
}
