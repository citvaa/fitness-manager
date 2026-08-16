package com.example.demo.service.impl.schedule;

import com.example.demo.dto.schedule.TrainerScheduleDTO;
import com.example.demo.enums.WorkStatus;
import com.example.demo.mapper.schedule.TrainerScheduleMapper;
import com.example.demo.model.schedule.GymSchedule;
import com.example.demo.model.schedule.TrainerSchedule;
import com.example.demo.model.user.Trainer;
import com.example.demo.model.user.User;
import com.example.demo.repository.schedule.GymScheduleRepository;
import com.example.demo.repository.schedule.TrainerScheduleRepository;
import com.example.demo.repository.user.TrainerRepository;
import com.example.demo.service.HolidayService;
import com.example.demo.service.params.request.schedule.CreateOwnTrainerScheduleRequest;
import com.example.demo.service.params.request.schedule.CreateOwnTrainerUnavailabilityRequest;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link TrainerScheduleServiceImpl} - the Faza 6 self-service schedule
 * endpoints (a trainer resolved from the JWT, never a client-supplied trainerId - see AGENTS.md
 * "Upgrade: Faza 6 decisions") and deleteSchedule's TRAINER-may-delete-own-only ownership check.
 * MANAGER no longer has any write access to a trainer's schedule at all (createSchedule/
 * createUnavailability were removed, and deleteSchedule's manager bypass was removed) - see
 * AGENTS.md "Upgrade: manager schedule-write removal decisions". The controller layer enforces
 * this by requiring @RoleRequired("TRAINER") on every write endpoint including DELETE /{id}, so
 * a MANAGER JWT can no longer reach deleteSchedule() at all in practice; the service-level test
 * below confirms the service itself is unusable for a caller with no Trainer profile too (defense
 * in depth, not reachable through the real API).
 */
@ExtendWith(MockitoExtension.class)
class TrainerScheduleServiceImplTest {

    @Mock
    private GymScheduleRepository gymScheduleRepository;
    @Mock
    private TrainerRepository trainerRepository;
    @Mock
    private TrainerScheduleRepository trainerScheduleRepository;
    @Mock
    private TrainerScheduleMapper trainerScheduleMapper;
    @Mock
    private HolidayService holidayService;

    private TrainerScheduleServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new TrainerScheduleServiceImpl(gymScheduleRepository, trainerRepository,
                trainerScheduleRepository, trainerScheduleMapper, holidayService);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAsTrainer(String email) {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("email", email)
                .claim("roles", List.of("TRAINER"))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(900))
                .build();
        Authentication authentication = new UsernamePasswordAuthenticationToken(jwt, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private void authenticateAsManager(String email) {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("email", email)
                .claim("roles", List.of("MANAGER"))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(900))
                .build();
        Authentication authentication = new UsernamePasswordAuthenticationToken(jwt, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    // ---------- createMySchedule ----------

    @Test
    void createMySchedule_resolvesTrainerFromJwt_neverFromRequestBody() {
        authenticateAsTrainer("trener@gym.com");
        Trainer trainer = Trainer.builder().id(7).build();
        when(trainerRepository.findByUserEmail("trener@gym.com")).thenReturn(Optional.of(trainer));

        LocalDate date = LocalDate.now().plusDays(1);
        CreateOwnTrainerScheduleRequest request = new CreateOwnTrainerScheduleRequest(
                date, LocalTime.of(9, 0), LocalTime.of(10, 0));

        GymSchedule gymSchedule = GymSchedule.builder()
                .day(date.getDayOfWeek()).openingTime(LocalTime.of(6, 0)).closingTime(LocalTime.of(22, 0)).build();
        when(gymScheduleRepository.findByDay(date.getDayOfWeek())).thenReturn(Optional.of(gymSchedule));
        when(holidayService.isGymClosedOn(date)).thenReturn(false);
        when(trainerScheduleRepository.existsByTrainerIdAndDateAndTimeRange(7, date, request.getStartTime(), request.getEndTime()))
                .thenReturn(false);
        when(trainerScheduleRepository.save(any(TrainerSchedule.class))).thenAnswer(inv -> inv.getArgument(0));
        when(trainerScheduleMapper.toDto(any(TrainerSchedule.class))).thenReturn(new TrainerScheduleDTO());

        service.createMySchedule(request);

        // CreateOwnTrainerScheduleRequest has no trainerId field at all - the trainer used for
        // the availability check/save can only have come from the JWT lookup above.
        verify(trainerScheduleRepository).existsByTrainerIdAndDateAndTimeRange(eq(7), any(), any(), any());
    }

    @Test
    void createMySchedule_throwsBareRuntimeExceptionWhenNoGymScheduleForThatDay() {
        // See AGENTS.md "Known issues"/GlobalExceptionHandler - this now maps to a 400 with a
        // real message at the controller layer instead of a bare 500, but the service itself
        // still throws a plain RuntimeException.
        authenticateAsTrainer("trener@gym.com");
        Trainer trainer = Trainer.builder().id(7).build();
        when(trainerRepository.findByUserEmail("trener@gym.com")).thenReturn(Optional.of(trainer));

        LocalDate sunday = nextDateWithDay(java.time.DayOfWeek.SUNDAY);
        CreateOwnTrainerScheduleRequest request = new CreateOwnTrainerScheduleRequest(
                sunday, LocalTime.of(9, 0), LocalTime.of(10, 0));
        when(gymScheduleRepository.findByDay(java.time.DayOfWeek.SUNDAY)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createMySchedule(request))
                .isInstanceOf(RuntimeException.class)
                .isNotInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Radno vreme teretane nije definisano za");

        verify(trainerScheduleRepository, never()).save(any());
    }

    @Test
    void createMySchedule_throwsIllegalArgumentWhenOutsideGymHours() {
        authenticateAsTrainer("trener@gym.com");
        Trainer trainer = Trainer.builder().id(7).build();
        when(trainerRepository.findByUserEmail("trener@gym.com")).thenReturn(Optional.of(trainer));

        LocalDate date = LocalDate.now().plusDays(1);
        CreateOwnTrainerScheduleRequest request = new CreateOwnTrainerScheduleRequest(
                date, LocalTime.of(5, 0), LocalTime.of(6, 0));
        GymSchedule gymSchedule = GymSchedule.builder()
                .day(date.getDayOfWeek()).openingTime(LocalTime.of(6, 0)).closingTime(LocalTime.of(22, 0)).build();
        when(gymScheduleRepository.findByDay(date.getDayOfWeek())).thenReturn(Optional.of(gymSchedule));
        when(holidayService.isGymClosedOn(date)).thenReturn(false);

        assertThatThrownBy(() -> service.createMySchedule(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("radnog vremena teretane");

        verify(trainerScheduleRepository, never()).save(any());
    }

    @Test
    void createMySchedule_throwsIllegalArgumentWhenStartAfterEnd() {
        authenticateAsTrainer("trener@gym.com");
        Trainer trainer = Trainer.builder().id(7).build();
        when(trainerRepository.findByUserEmail("trener@gym.com")).thenReturn(Optional.of(trainer));

        LocalDate date = LocalDate.now().plusDays(1);
        CreateOwnTrainerScheduleRequest request = new CreateOwnTrainerScheduleRequest(
                date, LocalTime.of(11, 0), LocalTime.of(10, 0));

        assertThatThrownBy(() -> service.createMySchedule(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Vreme početka je posle vremena završetka");

        verifyNoInteractions(gymScheduleRepository);
        verify(trainerScheduleRepository, never()).save(any());
    }

    @Test
    void createMySchedule_throwsWhenOverlappingExistingShift() {
        authenticateAsTrainer("trener@gym.com");
        Trainer trainer = Trainer.builder().id(7).build();
        when(trainerRepository.findByUserEmail("trener@gym.com")).thenReturn(Optional.of(trainer));

        LocalDate date = LocalDate.now().plusDays(1);
        CreateOwnTrainerScheduleRequest request = new CreateOwnTrainerScheduleRequest(
                date, LocalTime.of(9, 0), LocalTime.of(10, 0));
        GymSchedule gymSchedule = GymSchedule.builder()
                .day(date.getDayOfWeek()).openingTime(LocalTime.of(6, 0)).closingTime(LocalTime.of(22, 0)).build();
        when(gymScheduleRepository.findByDay(date.getDayOfWeek())).thenReturn(Optional.of(gymSchedule));
        when(holidayService.isGymClosedOn(date)).thenReturn(false);
        when(trainerScheduleRepository.existsByTrainerIdAndDateAndTimeRange(7, date, request.getStartTime(), request.getEndTime()))
                .thenReturn(true);

        assertThatThrownBy(() -> service.createMySchedule(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("preklapa");

        verify(trainerScheduleRepository, never()).save(any());
    }

    @Test
    void createMyUnavailability_savesOneRowPerDayInRange() {
        authenticateAsTrainer("trener@gym.com");
        Trainer trainer = Trainer.builder().id(7).build();
        when(trainerRepository.findByUserEmail("trener@gym.com")).thenReturn(Optional.of(trainer));

        CreateOwnTrainerUnavailabilityRequest request = new CreateOwnTrainerUnavailabilityRequest(
                LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 12), WorkStatus.VACATION);

        service.createMyUnavailability(request);

        verify(trainerScheduleRepository, times(3)).save(any(TrainerSchedule.class));
    }

    // ---------- deleteSchedule ownership ----------

    @Test
    void deleteSchedule_managerWithNoTrainerProfileCannotDeleteAnyEntry() {
        // MANAGER can no longer reach this at all via the real API (controller now requires
        // @RoleRequired("TRAINER")) - this confirms the service itself has no manager bypass
        // either: a caller with no Trainer profile of their own can't delete anyone's entry.
        authenticateAsManager("admin@gym.com");
        Trainer owner = Trainer.builder().id(1).build();
        TrainerSchedule schedule = TrainerSchedule.builder().id(50).trainer(owner).build();
        when(trainerScheduleRepository.findById(50)).thenReturn(Optional.of(schedule));
        when(trainerRepository.findByUserEmail("admin@gym.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteSchedule(50))
                .isInstanceOf(EntityNotFoundException.class);

        verify(trainerScheduleRepository, never()).delete(any());
    }

    @Test
    void deleteSchedule_trainerMayDeleteOwnEntry() {
        authenticateAsTrainer("trener@gym.com");
        Trainer self = Trainer.builder().id(7).build();
        when(trainerRepository.findByUserEmail("trener@gym.com")).thenReturn(Optional.of(self));
        TrainerSchedule schedule = TrainerSchedule.builder().id(50).trainer(self).build();
        when(trainerScheduleRepository.findById(50)).thenReturn(Optional.of(schedule));

        service.deleteSchedule(50);

        verify(trainerScheduleRepository).delete(schedule);
    }

    @Test
    void deleteSchedule_trainerCannotDeleteAnotherTrainersEntry() {
        authenticateAsTrainer("trener2@gym.com");
        Trainer caller = Trainer.builder().id(2).build();
        when(trainerRepository.findByUserEmail("trener2@gym.com")).thenReturn(Optional.of(caller));
        Trainer owner = Trainer.builder().id(1).build();
        TrainerSchedule schedule = TrainerSchedule.builder().id(50).trainer(owner).build();
        when(trainerScheduleRepository.findById(50)).thenReturn(Optional.of(schedule));

        assertThatThrownBy(() -> service.deleteSchedule(50))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("svoje");

        verify(trainerScheduleRepository, never()).delete(any());
    }

    @Test
    void deleteSchedule_throwsWhenEntryNotFound() {
        when(trainerScheduleRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteSchedule(99))
                .isInstanceOf(EntityNotFoundException.class);
    }

    private LocalDate nextDateWithDay(java.time.DayOfWeek target) {
        LocalDate date = LocalDate.now().plusDays(1);
        while (date.getDayOfWeek() != target) {
            date = date.plusDays(1);
        }
        return date;
    }
}
