package com.example.demo.service.impl;

import com.example.demo.dto.TrainerDTO;
import com.example.demo.enums.Role;
import com.example.demo.mapper.TrainerMapper;
import com.example.demo.model.Trainer;
import com.example.demo.model.User;
import com.example.demo.repository.TrainerRepository;
import com.example.demo.service.TrainerService;
import com.example.demo.service.UserService;
import com.example.demo.service.params.request.Trainer.CreateTrainerRequest;
import com.example.demo.service.params.request.User.CreateUserRequest;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
@RequiredArgsConstructor
@Service
public class TrainerServiceImpl implements TrainerService {
    
    private final UserService userService;
    private final TrainerRepository trainerRepository;
    private final TrainerMapper trainerMapper;

    @Transactional
    public TrainerDTO create(@NotNull CreateTrainerRequest request) {
        CreateUserRequest createUserRequest = new CreateUserRequest(request.getUsername());
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
}
