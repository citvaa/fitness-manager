package com.example.demo.service.impl;

import com.example.demo.dto.TrainerDTO;
import com.example.demo.dto.UserDTO;
import com.example.demo.enums.Role;
import com.example.demo.mapper.TrainerMapper;
import com.example.demo.mapper.UserMapper;
import com.example.demo.model.Trainer;
import com.example.demo.model.User;
import com.example.demo.repository.TrainerRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.TrainerService;
import com.example.demo.service.UserService;
import com.example.demo.service.params.request.Trainer.CreateTrainerRequest;
import com.example.demo.service.params.request.User.CreateUserRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@RequiredArgsConstructor
@Service
public class TrainerServiceImpl implements TrainerService {
    private final UserRepository userRepository;
    private final UserService userService;
    private final UserMapper userMapper;
    private final TrainerRepository trainerRepository;
    private final TrainerMapper trainerMapper;

    @Override
    public TrainerDTO create(CreateTrainerRequest request) {
        Optional<User> existingUser = userRepository.findByEmail(request.getEmail());

        User user;
        if (existingUser.isPresent()) {
            user = existingUser.get();
        } else {
            CreateUserRequest createUserRequest = new CreateUserRequest(request.getUsername(), request.getEmail());
            UserDTO newUserDto = userService.create(createUserRequest);
            user = userMapper.toEntity(newUserDto);
        }

        userService.addRole(user.getId(), Role.TRAINER);

        Trainer trainer = new Trainer();
        trainer.setUser(user);
        trainer.setEmploymentDate(request.getEmploymentDate());
        trainer.setBirthYear(request.getBirthYear());
        trainer.setStatus(request.getStatus());

        Trainer savedTrainer = trainerRepository.save(trainer);

        return trainerMapper.toDto(savedTrainer);
    }
}
