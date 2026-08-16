package com.example.demo.service.impl.user;

import com.example.demo.dto.summary.ClientSummaryDTO;
import com.example.demo.dto.user.TrainerDTO;
import com.example.demo.enums.Role;
import com.example.demo.mapper.user.TrainerMapper;
import com.example.demo.model.user.Trainer;
import com.example.demo.model.user.User;
import com.example.demo.repository.user.ClientAppointmentRepository;
import com.example.demo.repository.user.TrainerRepository;
import com.example.demo.repository.schedule.TrainerScheduleRepository;
import com.example.demo.service.user.TrainerService;
import com.example.demo.service.user.UserService;
import com.example.demo.service.params.request.user.trainer.CreateTrainerRequest;
import com.example.demo.service.params.request.user.CreateUserRequest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Transactional(readOnly = true)
@RequiredArgsConstructor
@Service
public class TrainerServiceImpl implements TrainerService {
    
    private final UserService userService;
    private final TrainerRepository trainerRepository;
    private final TrainerMapper trainerMapper;
    private final TrainerScheduleRepository trainerScheduleRepository;
    private final ClientAppointmentRepository clientAppointmentRepository;
    private final EntityManager entityManager;

    @Transactional
    @CachePut(value = "TRAINER_CACHE", key = "#result.id")
    public TrainerDTO create(@NotNull CreateTrainerRequest request) {
        CreateUserRequest createUserRequest = new CreateUserRequest(request.getEmail());
        User user = userService.findOrCreateUser(createUserRequest);
        user = entityManager.merge(user);

        userService.addRole(user.getId(), Role.TRAINER);

        Trainer trainer = Trainer.builder()
                .user(user)
                .employmentDate(request.getEmploymentDate())
                .birthYear(request.getBirthYear())
                .status(request.getStatus())
                .build();

        Trainer savedTrainer = trainerRepository.save(trainer);

        return trainerMapper.toDto(savedTrainer);
    }

    @Cacheable(value = "TRAINER_CACHE", key = "#id")
    public TrainerDTO getById(Integer id) {
        return trainerRepository.findById(id)
                .map(trainerMapper::toDto)
                .orElseThrow(() -> new EntityNotFoundException("Trener nije pronađen"));
    }

    @CachePut(value = "TRAINER_CACHE", key = "#id")
    @Transactional
    public TrainerDTO update(Integer id, CreateTrainerRequest request) {
        return trainerRepository.findById(id)
                .map(trainer -> {
                    trainer.getUser().setEmail(request.getEmail());
                    trainer.setEmploymentDate(request.getEmploymentDate());
                    trainer.setBirthYear(request.getBirthYear());
                    trainer.setStatus(request.getStatus());
                    Trainer savedTrainer = trainerRepository.save(trainer);
                    return trainerMapper.toDto(savedTrainer);
                }).orElseThrow(() -> new EntityNotFoundException("Korisnik nije pronađen"));
    }

    @CacheEvict(value = "TRAINER_CACHE", key = "#id")
    @Transactional
    public void delete(Integer id) {
        Trainer trainer = trainerRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Korisnik nije pronađen"));
        Integer userId = trainer.getUser().getId();

        trainerScheduleRepository.deleteByTrainer(trainer);

        trainerRepository.delete(trainer);

        // Deleting the Trainer domain row doesn't touch the User account or its roles on its
        // own (see AGENTS.md "Upgrade: Faza 6 decisions" - addRole/removeRole are the only thing
        // that manage the roles set) - without this, the account would be left with a dangling
        // TRAINER role and no matching domain row, the same class of gap the admin UI already
        // works around on the create/assign side. Uses removeRoleForProfileDeletion, not
        // removeRole - this is exactly the one legitimate case of ending up with zero operational
        // roles (see AGENTS.md "Upgrade: operational-role cardinality decisions").
        userService.removeRoleForProfileDeletion(userId, Role.TRAINER);
    }

    public List<TrainerDTO> getAll() {
        return trainerMapper.toDto(trainerRepository.findAll());
    }

    public List<ClientSummaryDTO> getMyClients() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new AccessDeniedException("Neovlašćen pristup!");
        }
        String email = jwt.getClaim("email");
        Trainer trainer = trainerRepository.findByUserEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Trener nije pronađen za prijavljenog korisnika!"));

        return clientAppointmentRepository.findDistinctClientsByAppointmentTrainerId(trainer.getId()).stream()
                .map(client -> new ClientSummaryDTO(client.getId(), client.getUser().getEmail()))
                .toList();
    }

    public TrainerDTO getMe() {
        return trainerMapper.toDto(getAuthenticatedTrainer());
    }

    private Trainer getAuthenticatedTrainer() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new AccessDeniedException("Neovlašćen pristup!");
        }
        String email = jwt.getClaim("email");
        return trainerRepository.findByUserEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Trener nije pronađen za prijavljenog korisnika!"));
    }

}
