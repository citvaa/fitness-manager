package com.example.demo.service.impl.user;

import com.example.demo.dto.user.TrainerDTO;
import com.example.demo.enums.EmploymentStatus;
import com.example.demo.enums.Role;
import com.example.demo.mapper.user.TrainerMapper;
import com.example.demo.model.user.Trainer;
import com.example.demo.model.user.User;
import com.example.demo.repository.schedule.TrainerScheduleRepository;
import com.example.demo.repository.user.ClientAppointmentRepository;
import com.example.demo.repository.user.TrainerRepository;
import com.example.demo.service.params.request.user.CreateUserRequest;
import com.example.demo.service.params.request.user.trainer.CreateTrainerRequest;
import com.example.demo.service.user.UserService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link TrainerServiceImpl} CRUD - create/update/delete, including the Faza 6
 * fix where {@code delete()} also strips the TRAINER role from the underlying account (see
 * AGENTS.md "Upgrade: Faza 6 decisions (continued, part 2)"). No test coverage existed for this
 * service before Faza 9 - see AGENTS.md "Upgrade: Faza 9 decisions".
 */
@ExtendWith(MockitoExtension.class)
class TrainerServiceImplTest {

    @Mock
    private UserService userService;
    @Mock
    private TrainerRepository trainerRepository;
    @Mock
    private TrainerMapper trainerMapper;
    @Mock
    private TrainerScheduleRepository trainerScheduleRepository;
    @Mock
    private ClientAppointmentRepository clientAppointmentRepository;
    @Mock
    private EntityManager entityManager;

    private TrainerServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new TrainerServiceImpl(userService, trainerRepository, trainerMapper,
                trainerScheduleRepository, clientAppointmentRepository, entityManager);
    }

    private CreateTrainerRequest request() {
        return new CreateTrainerRequest("trener@gym.com", LocalDate.of(2024, 1, 1), 1990, EmploymentStatus.FULL_TIME);
    }

    @Test
    void create_findsOrCreatesUserAddsTrainerRoleAndBuildsTrainer() {
        User user = User.builder().id(1).email("trener@gym.com").build();
        when(userService.findOrCreateUser(any(CreateUserRequest.class))).thenReturn(user);
        when(entityManager.merge(user)).thenReturn(user);
        when(trainerRepository.save(any(Trainer.class))).thenAnswer(inv -> inv.getArgument(0));
        when(trainerMapper.toDto(any(Trainer.class))).thenReturn(new TrainerDTO());

        service.create(request());

        verify(userService).addRole(1, Role.TRAINER);

        ArgumentCaptor<Trainer> captor = ArgumentCaptor.forClass(Trainer.class);
        verify(trainerRepository).save(captor.capture());
        assertThat(captor.getValue().getUser()).isSameAs(user);
        assertThat(captor.getValue().getBirthYear()).isEqualTo(1990);
        assertThat(captor.getValue().getStatus()).isEqualTo(EmploymentStatus.FULL_TIME);
    }

    @Test
    void update_updatesEmailAndProfileFields() {
        User user = User.builder().id(1).email("old@gym.com").build();
        Trainer existing = Trainer.builder().id(9).user(user).status(EmploymentStatus.CONTRACT).build();
        when(trainerRepository.findById(9)).thenReturn(Optional.of(existing));
        when(trainerRepository.save(any(Trainer.class))).thenAnswer(inv -> inv.getArgument(0));
        when(trainerMapper.toDto(any(Trainer.class))).thenReturn(new TrainerDTO());

        service.update(9, request());

        assertThat(user.getEmail()).isEqualTo("trener@gym.com");
        assertThat(existing.getStatus()).isEqualTo(EmploymentStatus.FULL_TIME);
    }

    @Test
    void update_throwsWhenTrainerNotFound() {
        when(trainerRepository.findById(9)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(9, request()))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void delete_removesTrainerRowScheduleAndRoleFromAccount() {
        User user = User.builder().id(1).build();
        Trainer trainer = Trainer.builder().id(9).user(user).build();
        when(trainerRepository.findById(9)).thenReturn(Optional.of(trainer));

        service.delete(9);

        verify(trainerScheduleRepository).deleteByTrainer(trainer);
        verify(trainerRepository).delete(trainer);
        // The mirror-image of the addRole-doesn't-create-domain-rows gap: deleting the domain
        // row must also strip the TRAINER role, or the account is left with a dangling role and
        // no matching Trainer row. See AGENTS.md "Upgrade: Faza 6 decisions (continued, part 2)".
        // Uses removeRoleForProfileDeletion (not removeRole) - see AGENTS.md "Upgrade:
        // operational-role cardinality decisions" for why this legitimately leaves zero
        // operational roles, unlike the generic role-removal endpoint.
        verify(userService).removeRoleForProfileDeletion(1, Role.TRAINER);
    }

    @Test
    void delete_throwsWhenTrainerNotFound() {
        when(trainerRepository.findById(9)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(9)).isInstanceOf(EntityNotFoundException.class);

        verify(userService, never()).removeRoleForProfileDeletion(any(), any());
    }

    @Test
    void getAll_mapsEveryTrainer() {
        List<Trainer> trainers = List.of(Trainer.builder().id(1).build(), Trainer.builder().id(2).build());
        when(trainerRepository.findAll()).thenReturn(trainers);
        when(trainerMapper.toDto(trainers)).thenReturn(List.of(new TrainerDTO(), new TrainerDTO()));

        assertThat(service.getAll()).hasSize(2);
    }
}
