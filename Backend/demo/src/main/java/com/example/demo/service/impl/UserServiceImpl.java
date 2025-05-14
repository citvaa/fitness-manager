package com.example.demo.service.impl;

import com.example.demo.dto.UserDTO;
import com.example.demo.mapper.UserMapper;
import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.params.request.CreateUserRequest;
import com.example.demo.service.params.request.RegisterUserRequest;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserServiceImpl implements com.example.demo.service.UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository, UserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    public List<UserDTO> getAll() {
        List<User> users = userRepository.findAll();
        return userMapper.toDTOList(users);
    }

    public Optional<UserDTO> getById(Integer id) {
        return Optional.ofNullable(userRepository.findById(id)
                .map(userMapper::toDto)
                .orElseThrow(() -> new EntityNotFoundException("User not found")));
    }

    public UserDTO create(CreateUserRequest request) {
        UUID registration_key = UUID.randomUUID();
        LocalDateTime registration_key_validity = LocalDateTime.now().plusDays(1);

        User user = userMapper.toEntity(request);
        user.setRegistrationKey(registration_key);
        user.setRegistrationKeyValidity(registration_key_validity);
        user.setIsActivated(false);

        User savedUser = userRepository.save(user);
        return userMapper.toDto(savedUser);
    }

    public UserDTO update(Integer id, CreateUserRequest request) {
        return userRepository.findById(id)
                .map(user -> {
                    user.setUsername(request.getUsername());
                    //user.setPassword(request.getPassword());
                    user.setEmail(request.getEmail());
                    User savedUser = userRepository.save(user);
                    return userMapper.toDto(savedUser);
                }).orElseThrow(() -> new RuntimeException("User not found"));
    }

    public void delete(Integer id) {
        userRepository.deleteById(id);
    }

    public void register(RegisterUserRequest request) {
        userRepository.findByRegistrationKey(request.getRegistrationKey())
                .filter(user -> user.getRegistrationKeyValidity().isAfter(LocalDateTime.now()))
                .ifPresent(user -> {
                    String hashedPassword = passwordEncoder.encode(request.getPassword());
                    userRepository.updatePasswordByRegistrationKey(request.getRegistrationKey(), hashedPassword);
                    userRepository.clearRegistrationKey(user.getId());
                    userRepository.activateUser(user.getId());
                });
    }
}
