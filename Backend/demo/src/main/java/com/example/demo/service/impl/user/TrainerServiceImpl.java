package com.example.demo.service.impl.user;

import com.example.demo.dto.user.TrainerDTO;
import com.example.demo.enums.Role;
import com.example.demo.mapper.user.TrainerMapper;
import com.example.demo.model.user.Trainer;
import com.example.demo.model.user.User;
import com.example.demo.repository.user.TrainerRepository;
import com.example.demo.repository.schedule.TrainerScheduleRepository;
import com.example.demo.service.user.TrainerService;
import com.example.demo.service.user.UserService;
import com.example.demo.service.params.request.user.trainer.CreateTrainerRequest;
import com.example.demo.service.params.request.user.CreateUserRequest;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
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

    @Transactional
    @CachePut(value = "TRAINER_CACHE", key = "#result.id")
    public TrainerDTO create(@NotNull CreateTrainerRequest request) {
        CreateUserRequest createUserRequest = new CreateUserRequest(request.getEmail());
        User user = userService.findOrCreateUser(createUserRequest);

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
                .orElseThrow(() -> new EntityNotFoundException("Trainer not found"));
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
                }).orElseThrow(() -> new EntityNotFoundException("User not found"));
    }

    @CacheEvict(value = "TRAINER_CACHE", key = "#id")
    @Transactional
    public void delete(Integer id) {
        Trainer trainer = trainerRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        trainerScheduleRepository.deleteByTrainer(trainer);

        trainerRepository.delete(trainer);
    }

    public List<TrainerDTO> getAll() {
        return trainerMapper.toDto(trainerRepository.findAll());
    }

}
