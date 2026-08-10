package com.example.demo.service.impl.schedule;

import com.example.demo.dto.schedule.TrainerScheduleDTO;
import com.example.demo.enums.WorkStatus;
import com.example.demo.mapper.schedule.TrainerScheduleMapper;
import com.example.demo.model.schedule.GymSchedule;
import com.example.demo.model.user.Trainer;
import com.example.demo.model.schedule.TrainerSchedule;
import com.example.demo.repository.schedule.GymScheduleRepository;
import com.example.demo.repository.user.TrainerRepository;
import com.example.demo.repository.schedule.TrainerScheduleRepository;
import com.example.demo.service.HolidayService;
import com.example.demo.service.schedule.TrainerScheduleService;
import com.example.demo.service.params.request.schedule.CreateOwnTrainerScheduleRequest;
import com.example.demo.service.params.request.schedule.CreateOwnTrainerUnavailabilityRequest;
import com.example.demo.service.params.request.schedule.CreateTrainerScheduleRequest;
import com.example.demo.service.params.request.schedule.CreateTrainerUnavailabilityRequest;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Transactional(readOnly = true)
@RequiredArgsConstructor
@Service
public class TrainerScheduleServiceImpl implements TrainerScheduleService {
    private final GymScheduleRepository gymScheduleRepository;
    private final TrainerRepository trainerRepository;
    private final TrainerScheduleRepository trainerScheduleRepository;
    private final TrainerScheduleMapper trainerScheduleMapper;
    private final HolidayService holidayService;

    @Transactional
    public TrainerScheduleDTO createSchedule(@NotNull CreateTrainerScheduleRequest request) {
        validateScheduleRequest(request.getDate(), request.getStartTime(), request.getEndTime());
        validateGymHours(request.getDate(), request.getStartTime(), request.getEndTime());
        validateTrainerAvailability(request.getTrainerId(), request.getDate(), request.getStartTime(), request.getEndTime());

        Trainer trainer = fetchTrainer(request.getTrainerId());
        TrainerSchedule trainerSchedule = buildTrainerSchedule(trainer, request.getDate(), request.getStartTime(), request.getEndTime());

        return trainerScheduleMapper.toDto(trainerScheduleRepository.save(trainerSchedule));
    }

    @Transactional
    public void createUnavailability(@NotNull CreateTrainerUnavailabilityRequest request) {
        Trainer trainer = trainerRepository.findById(request.getTrainerId())
                .orElseThrow(() -> new EntityNotFoundException("Trener nije pronađen"));
        saveUnavailabilityRange(trainer, request.getStartDate(), request.getEndDate(), request.getStatus());
    }

    @Transactional
    public TrainerScheduleDTO createMySchedule(@NotNull CreateOwnTrainerScheduleRequest request) {
        Trainer trainer = getAuthenticatedTrainer();

        validateScheduleRequest(request.getDate(), request.getStartTime(), request.getEndTime());
        validateGymHours(request.getDate(), request.getStartTime(), request.getEndTime());
        validateTrainerAvailability(trainer.getId(), request.getDate(), request.getStartTime(), request.getEndTime());

        TrainerSchedule trainerSchedule = buildTrainerSchedule(trainer, request.getDate(), request.getStartTime(), request.getEndTime());
        return trainerScheduleMapper.toDto(trainerScheduleRepository.save(trainerSchedule));
    }

    @Transactional
    public void createMyUnavailability(@NotNull CreateOwnTrainerUnavailabilityRequest request) {
        Trainer trainer = getAuthenticatedTrainer();
        saveUnavailabilityRange(trainer, request.getStartDate(), request.getEndDate(), request.getStatus());
    }

    public List<TrainerScheduleDTO> getSchedule(Integer trainerId) {
        return trainerScheduleMapper.toDto(trainerScheduleRepository.findByTrainerIdOrderByDateAsc(trainerId));
    }

    public List<TrainerScheduleDTO> getMySchedule() {
        Trainer trainer = getAuthenticatedTrainer();
        return trainerScheduleMapper.toDto(trainerScheduleRepository.findByTrainerIdOrderByDateAsc(trainer.getId()));
    }

    @Transactional
    public void deleteSchedule(Integer id) {
        TrainerSchedule schedule = trainerScheduleRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Stavka rasporeda trenera nije pronađena"));

        if (!isManager()) {
            Trainer trainer = getAuthenticatedTrainer();
            if (!schedule.getTrainer().getId().equals(trainer.getId())) {
                throw new AccessDeniedException("Možete brisati samo svoje stavke rasporeda");
            }
        }

        trainerScheduleRepository.delete(schedule);
    }

    private void saveUnavailabilityRange(Trainer trainer, LocalDate startDate, LocalDate endDate, WorkStatus status) {
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Datum početka ne može biti posle datuma završetka");
        }

        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            // Same overlap check used for a WORKING shift (validateTrainerAvailability) - a
            // full-day unavailability entry must not be creatable on a day where this trainer
            // already has ANY other schedule entry (WORKING or otherwise), the same "radni i
            // neradni period se ne preklapaju" rule from the other direction. Previously this
            // loop saved unconditionally with no check at all - see AGENTS.md ("Upgrade:
            // manager-testing fixes").
            validateTrainerAvailability(trainer.getId(), currentDate, LocalTime.of(0, 0), LocalTime.of(23, 59));

            TrainerSchedule schedule = new TrainerSchedule();
            schedule.setTrainer(trainer);
            schedule.setDate(currentDate);
            schedule.setStartTime(LocalTime.of(0, 0));
            schedule.setEndTime(LocalTime.of(23, 59));
            schedule.setStatus(status);

            trainerScheduleRepository.save(schedule);
            currentDate = currentDate.plusDays(1);
        }
    }

    /** Same JWT->email->repository idiom used across the codebase (see AGENTS.md, "Upgrade: service layer decisions"). */
    private Trainer getAuthenticatedTrainer() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new AccessDeniedException("Neovlašćen pristup!");
        }
        String email = jwt.getClaim("email");
        return trainerRepository.findByUserEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Trener nije pronađen za prijavljenog korisnika!"));
    }

    private boolean isManager() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            return false;
        }
        List<String> roles = jwt.getClaimAsStringList("roles");
        return roles != null && roles.contains("MANAGER");
    }

    private void validateScheduleRequest(LocalDate date, @NotNull LocalTime startTime, LocalTime endTime) {
        if (startTime.isAfter(endTime)) {
            throw new IllegalArgumentException("Vreme početka je posle vremena završetka");
        }

        if (date.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Datum rasporeda ne može biti u prošlosti!");
        }
    }

    private void validateGymHours(@NotNull LocalDate date, LocalTime startTime, LocalTime endTime) {
        GymSchedule gymSchedule = gymScheduleRepository.findByDay(date.getDayOfWeek())
                .orElseThrow(() -> new RuntimeException("Radno vreme teretane nije definisano za " + date));

        if (holidayService.isGymClosedOn(date)) {
            throw new IllegalArgumentException("Teretana je zatvorena " + date);
        }

        if (startTime.isBefore(gymSchedule.getOpeningTime()) || endTime.isAfter(gymSchedule.getClosingTime())) {
            throw new IllegalArgumentException("Raspored trenera mora biti u okviru radnog vremena teretane: " + gymSchedule.getOpeningTime() + " - " + gymSchedule.getClosingTime());
        }
    }

    private void validateTrainerAvailability(Integer trainerId, LocalDate date, LocalTime startTime, LocalTime endTime) {
        boolean overlapExists = trainerScheduleRepository.existsByTrainerIdAndDateAndTimeRange(trainerId, date, startTime, endTime);
        if (overlapExists) {
            throw new IllegalArgumentException("Trener već ima smenu koja se preklapa sa ovim vremenskim periodom");
        }
    }

    private Trainer fetchTrainer(Integer trainerId) {
        return trainerRepository.findById(trainerId)
                .orElseThrow(() -> new RuntimeException("Trener nije pronađen"));
    }

    private TrainerSchedule buildTrainerSchedule(Trainer trainer, LocalDate date, LocalTime startTime, LocalTime endTime) {
        return TrainerSchedule.builder()
                .trainer(trainer)
                .date(date)
                .startTime(startTime)
                .endTime(endTime)
                .status(WorkStatus.WORKING)
                .build();
    }
}
