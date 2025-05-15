package com.example.demo.service.impl;

import com.example.demo.configuration.AppConfig;
import com.example.demo.dto.UserDTO;
import com.example.demo.mapper.UserMapper;
import com.example.demo.model.Role;
import com.example.demo.model.User;
import com.example.demo.repository.RoleRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.params.request.User.CreateUserRequest;
import com.example.demo.service.params.request.User.LoginUserRequest;
import com.example.demo.service.params.request.User.RegisterUserRequest;
import com.example.demo.service.params.request.User.ResetPasswordRequest;
import com.example.demo.util.JwtUtil;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class UserServiceImpl implements com.example.demo.service.UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final AppConfig appConfig;
    private final RoleRepository roleRepository;
    private final JwtUtil jwtUtil;

    public Page<UserDTO> getUsers(int page, int size, String sortBy) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy));
        return userRepository.findAll(pageable).map(userMapper::toDto);
    }

    public Optional<UserDTO> getById(Integer id) {
        return Optional.ofNullable(userRepository.findById(id)
                .map(userMapper::toDto)
                .orElseThrow(() -> new EntityNotFoundException("User not found")));
    }

    public UserDTO create(CreateUserRequest request) {
        String registration_key = UUID.randomUUID().toString();
        LocalDateTime registration_key_validity = LocalDateTime.now().plusMinutes(appConfig.getRegistrationKeyValidityMinutes());

        User user = userMapper.toEntity(request);
        user.setRegistrationKey(registration_key);
        user.setRegistrationKeyValidity(registration_key_validity);
        user.setIsActivated(false);

        //ovde se salje key na mejl

        User savedUser = userRepository.save(user);
        return userMapper.toDto(savedUser);
    }

    public UserDTO update(Integer id, CreateUserRequest request) {
        return userRepository.findById(id)
                .map(user -> {
                    user.setUsername(request.getUsername());
                    user.setEmail(request.getEmail());
                    User savedUser = userRepository.save(user);
                    return userMapper.toDto(savedUser);
                }).orElseThrow(() -> new EntityNotFoundException("User not found"));
    }

    public void delete(Integer id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        userRepository.delete(user);
    }

    public void register(RegisterUserRequest request) {
        userRepository.findByRegistrationKey(request.getRegistrationKey())
                .filter(user -> user.getRegistrationKeyValidity().isAfter(LocalDateTime.now()))
                .ifPresent(user -> {
                    String hashedPassword = passwordEncoder.encode(request.getPassword());
                    user.setPassword(hashedPassword);
                    user.setRegistrationKey(null);
                    user.setRegistrationKeyValidity(null);
                    user.setIsActivated(true);
                    userRepository.save(user);
                });
    }

    public String login(LoginUserRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("Wrong password");
        }

        return jwtUtil.generateToken(user);
    }

    public void requestPasswordReset(String email) {
        String resetKey = UUID.randomUUID().toString();
        LocalDateTime resetKeyValidity = LocalDateTime.now().plusMinutes(appConfig.getResetKeyValidityMinutes());

        userRepository.findByEmail(email).ifPresent(user -> {
            user.setResetKey(resetKey);
            user.setResetTokenValidity(resetKeyValidity);
            userRepository.save(user);
            //ovde se salje token na mejl
        });
    }

    public void resetPassword(ResetPasswordRequest request) {
        userRepository.findByResetKey(request.getResetKey())
                .ifPresent(user -> {
                    String hashedPassword = passwordEncoder.encode(request.getPassword());
                    user.setPassword(hashedPassword);
                    user.setResetKey(null);
                    user.setResetTokenValidity(null);
                    userRepository.save(user);
                });
    }

    @Transactional
    public void addRole(Integer userId, String roleName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new EntityNotFoundException("Role not found"));

        user.getRoles().add(role);
        userRepository.save(user);
    }

    @Transactional
    public void removeRole(Integer userId, String roleName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new EntityNotFoundException("Role not found"));

        user.getRoles().remove(role);
        userRepository.save(user);
    }

}
