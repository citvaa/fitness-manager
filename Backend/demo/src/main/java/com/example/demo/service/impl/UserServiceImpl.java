package com.example.demo.service.impl;

import com.example.demo.configuration.AppConfig;
import com.example.demo.dto.UserDTO;
import com.example.demo.enums.Role;
import com.example.demo.mapper.UserMapper;
import com.example.demo.model.User;
import com.example.demo.model.UserRole;
import com.example.demo.repository.UserRepository;
import com.example.demo.repository.UserRoleRepository;
import com.example.demo.service.EmailService;
import com.example.demo.service.params.request.Email.ActivationEmailData;
import com.example.demo.service.params.request.Email.ForgetPasswordEmailData;
import com.example.demo.repository.*;
import com.example.demo.service.params.request.User.*;
import com.example.demo.service.params.response.User.LoginResponse;
import com.example.demo.util.DateTimeUtil;
import com.example.demo.util.JwtUtil;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.UUID;

@Transactional(readOnly = true)
@RequiredArgsConstructor
@Service
public class UserServiceImpl implements com.example.demo.service.UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final AppConfig appConfig;
    private final JwtUtil jwtUtil;
    private final UserRoleRepository userRoleRepository;
    private final TrainerRepository trainerRepository;
    private final ClientRepository clientRepository;
    private final PaymentRepository paymentRepository;
    private final EmailService emailService;

    public Page<UserDTO> getUsers(@NotNull SearchUserRequest request) {
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize(), Sort.by(request.getSortBy()));

        if (request.getSearch() == null || request.getSearch().isEmpty()) {
            return userRepository.findAll(pageable).map(userMapper::toDto);
        }

        return userRepository.findByEmailContaining(request.getSearch(), pageable).map(userMapper::toDto);
    }

    public Optional<UserDTO> getById(Integer id) {
        return Optional.ofNullable(userRepository.findById(id)
                .map(userMapper::toDto)
                .orElseThrow(() -> new EntityNotFoundException("User not found")));
    }

    @Transactional
    public UserDTO create(@NotNull CreateUserRequest request) {
        String registration_key = UUID.randomUUID().toString();
        LocalDateTime registration_key_validity = LocalDateTime.now().plusMinutes(appConfig.getRegistrationKeyValidityMinutes());

        User user = User.builder()
                .email(request.getEmail())
                .registrationKey(registration_key)
                .registrationKeyValidity(registration_key_validity)
                .isActivated(false)
                .userRoles(new HashSet<>())
                .build();

        ActivationEmailData emailData = ActivationEmailData.builder()
                .registrationKey(registration_key)
                .registrationKeyValidity(DateTimeUtil.formatTime(registration_key_validity))
                .build();
        emailService.sendActivationEmail(request.getEmail(), emailData);

        User savedUser = userRepository.save(user);
        return userMapper.toDto(savedUser);
    }

    @Transactional
    public UserDTO update(Integer id, CreateUserRequest request) {
        return userRepository.findById(id)
                .map(user -> {
                    user.setEmail(request.getEmail());
                    User savedUser = userRepository.save(user);
                    return userMapper.toDto(savedUser);
                }).orElseThrow(() -> new EntityNotFoundException("User not found"));
    }

    @Transactional
    public void delete(Integer id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        paymentRepository.deleteByUser(user);
        clientRepository.deleteByUser(user);
        trainerRepository.deleteByUser(user);
        userRoleRepository.deleteByUser(user);

        userRepository.delete(user);
    }

    @Transactional
    public void register(@NotNull RegisterUserRequest request) {
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

    public LoginResponse login(@NotNull LoginUserRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("Wrong password");
        }

        String token = jwtUtil.generateToken(user);
        LocalDateTime expiresAt = jwtUtil.getExpirationTime(token);

        return new LoginResponse(token, expiresAt);
    }

    @Transactional
    public void requestPasswordReset(String email) {
        String resetKey = UUID.randomUUID().toString();
        LocalDateTime resetKeyValidity = LocalDateTime.now().plusMinutes(appConfig.getResetKeyValidityMinutes());

        userRepository.findByEmail(email).ifPresent(user -> {
            user.setResetKey(resetKey);
            user.setResetKeyValidity(resetKeyValidity);
            userRepository.save(user);

            ForgetPasswordEmailData emailData = ForgetPasswordEmailData.builder()
                    .resetKey(resetKey)
                    .resetKeyValidity(DateTimeUtil.formatTime(resetKeyValidity))
                    .build();
            emailService.sendResetPasswordEmail(email, emailData);
        });
    }

    @Transactional
    public void resetPassword(@NotNull ResetPasswordRequest request) {
        userRepository.findByResetKey(request.getResetKey())
                .ifPresent(user -> {
                    String hashedPassword = passwordEncoder.encode(request.getPassword());
                    user.setPassword(hashedPassword);
                    user.setResetKey(null);
                    user.setResetKeyValidity(null);
                    userRepository.save(user);
                });
    }

    @Transactional
    public void addRole(Integer userId, Role role) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        boolean alreadyHasRole = user.getUserRoles() != null
                && user.getUserRoles().stream().anyMatch(userRole -> userRole.getRole().equals(role));

        if (alreadyHasRole) {
            throw new IllegalArgumentException("User already has role " + role);
        }

        UserRole userRole = new UserRole();
        userRole.setUser(user);
        userRole.setRole(role);

        userRoleRepository.save(userRole);
        user.getUserRoles().add(userRole);

        userRepository.save(user);
    }

    @Transactional
    public void removeRole(Integer userId, Role role) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        Optional<UserRole> userRoleToRemove = user.getUserRoles().stream().filter(userRole -> userRole.getRole().equals(role)).findFirst();

        if (userRoleToRemove.isEmpty()) {
            throw new IllegalArgumentException("User does not have role " + role);
        }

        userRoleRepository.delete(userRoleToRemove.get());
        user.getUserRoles().remove(userRoleToRemove.get());

        userRepository.save(user);
    }

    @Transactional
    public User findOrCreateUser(@NotNull CreateUserRequest request) {
        return userRepository.findByEmail(request.getEmail())
                .orElseGet(() -> {
                    CreateUserRequest createUserRequest = new CreateUserRequest(request.getEmail());
                    UserDTO newUserDto = create(createUserRequest);
                    return userMapper.toEntity(newUserDto);
                });
    }

}
