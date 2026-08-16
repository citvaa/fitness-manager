package com.example.demo.service.impl.user;

import com.example.demo.config.core.AppConfig;
import com.example.demo.dto.user.UserDTO;
import com.example.demo.enums.NotificationPreference;
import com.example.demo.enums.Role;
import com.example.demo.mapper.user.UserMapper;
import com.example.demo.model.user.User;
import com.example.demo.model.user.UserRole;
import com.example.demo.repository.user.ClientRepository;
import com.example.demo.repository.user.TrainerRepository;
import com.example.demo.repository.user.UserRepository;
import com.example.demo.repository.user.UserRoleRepository;
import com.example.demo.service.notification.email.EmailService;
import com.example.demo.service.notification.NotificationService;
import com.example.demo.service.params.request.email.ActivationEmailData;
import com.example.demo.service.params.request.email.ForgetPasswordEmailData;
import com.example.demo.repository.*;
import com.example.demo.service.params.request.user.*;
import com.example.demo.service.params.response.user.AuthResponse;
import com.example.demo.service.user.UserService;
import com.example.demo.util.DateTimeUtil;
import com.example.demo.util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.UUID;
import java.util.EnumSet;

@Transactional(readOnly = true)
@RequiredArgsConstructor
@Service
public class UserServiceImpl implements UserService {

    private static final EnumSet<Role> OPERATIONAL_ROLES = EnumSet.of(Role.MANAGER, Role.TRAINER, Role.CLIENT);

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
    private final NotificationService notificationService;

    public Page<UserDTO> getUsers(@NotNull SearchUserRequest request) {
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize(), Sort.by(request.getSortBy()));

        if (request.getRole() != null) {
            return userRepository.findDistinctByUserRolesRoleAndEmailContaining(
                    request.getRole(), request.getSearch() == null ? "" : request.getSearch(), pageable).map(userMapper::toDto);
        }

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
        if (request.getRole() != null) requireAdminForManagerRole(request.getRole());
        String registration_key = UUID.randomUUID().toString();
        LocalDateTime registration_key_validity = LocalDateTime.now().plusMinutes(appConfig.getRegistrationKeyValidityMinutes());

        User user = User.builder()
                .email(request.getEmail())
                .password(null)
                .notificationPreference(NotificationPreference.PUSH)
                .registrationKey(registration_key)
                .registrationKeyValidity(registration_key_validity)
                .isActivated(false)
                .userRoles(new HashSet<>())
                .build();

        User savedUser = userRepository.save(user);
        if (request.getRole() != null) {
            UserRole role = new UserRole();
            role.setUser(savedUser);
            role.setRole(request.getRole());
            userRoleRepository.save(role);
            savedUser.getUserRoles().add(role);
        }
        userRepository.flush();
        ActivationEmailData emailData = ActivationEmailData.builder()
                .registrationKey(registration_key)
                .registrationKeyValidity(DateTimeUtil.formatTime(registration_key_validity))
                .frontendUrl(appConfig.getFrontendUrl())
                .build();
        emailService.sendActivationEmail(request.getEmail(), emailData);
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

        user.getUserRoles().clear();
        userRepository.flush();
        paymentRepository.deleteByUser(user);
        clientRepository.deleteByUser(user);
        trainerRepository.deleteByUser(user);

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
                    notificationService.sendManagerAlert("Novi korisnik se registrovao: " + user.getEmail());
                });
    }

    public AuthResponse login(@NotNull LoginUserRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("Wrong password");
        }

        String accessToken = jwtUtil.generateAccessToken(user);
        LocalDateTime accessTokenExpirationTime = jwtUtil.getTokenExpirationTime(accessToken);
        String refreshToken = jwtUtil.generateRefreshToken(user);
        LocalDateTime refreshTokenExpirationTime = jwtUtil.getTokenExpirationTime(refreshToken);

        return new AuthResponse(accessToken, accessTokenExpirationTime, refreshToken, refreshTokenExpirationTime);
    }

    public AuthResponse login(String refreshToken) {
        LocalDateTime refreshTokenExpirationTime = jwtUtil.getTokenExpirationTime(refreshToken);

        if (refreshTokenExpirationTime.isBefore(LocalDateTime.now())) {
            throw new BadCredentialsException("Refresh token expired, please log in again");
        }

        Claims claims = jwtUtil.parseToken(refreshToken);
        String userId = claims.getSubject();
        User user = userRepository.findById(Integer.parseInt(userId))
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        String accessToken = jwtUtil.generateAccessToken(user);
        LocalDateTime accessTokenExpirationTime = jwtUtil.getTokenExpirationTime(accessToken);

        return new AuthResponse(accessToken, accessTokenExpirationTime, refreshToken, refreshTokenExpirationTime);
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
                    .frontendUrl(appConfig.getFrontendUrl())
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
    public void addRole(Integer id, Role role) {
        rejectAdminMutation(role);
        requireAdminForManagerRole(role);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        boolean alreadyHasRole = user.getUserRoles() != null
                && user.getUserRoles().stream().anyMatch(userRole -> userRole.getRole().equals(role));

        if (alreadyHasRole) {
            throw new IllegalArgumentException("User already has role " + role);
        }
        if (OPERATIONAL_ROLES.contains(role) && operationalRoleCount(user) > 0) {
            throw new IllegalArgumentException("User must have exactly one operational role; change the profile atomically instead");
        }

        UserRole userRole = new UserRole();
        userRole.setUser(user);
        userRole.setRole(role);

        userRoleRepository.save(userRole);
        user.getUserRoles().add(userRole);

        userRepository.save(user);
    }

    @Transactional
    public void removeRole(Integer id, Role role) {
        rejectAdminMutation(role);
        requireAdminForManagerRole(role);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        Optional<UserRole> userRoleToRemove = user.getUserRoles().stream().filter(userRole -> userRole.getRole().equals(role)).findFirst();

        if (userRoleToRemove.isEmpty()) {
            throw new IllegalArgumentException("User does not have role " + role);
        }
        if (OPERATIONAL_ROLES.contains(role) && operationalRoleCount(user) <= 1) {
            throw new IllegalArgumentException("User must retain exactly one operational role");
        }

        userRoleRepository.delete(userRoleToRemove.get());
        user.getUserRoles().remove(userRoleToRemove.get());

        userRepository.save(user);
    }

    @Transactional
    public User findOrCreateUser(@NotNull CreateUserRequest request) {
        return userRepository.findByEmail(request.getEmail())
                .orElseGet(() -> {
                    create(request);
                    return userRepository.findByEmail(request.getEmail())
                            .orElseThrow(() -> new IllegalStateException("Created user could not be reloaded"));
                });
    }

    @Transactional
    public void updateNotificationPreference(Integer id, NotificationPreference notificationPreference) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        user.setNotificationPreference(notificationPreference);
        userRepository.save(user);
    }

    public UserDTO getCurrentUser() { return userMapper.toDto(currentUser()); }

    @Transactional
    public void updateCurrentNotificationPreference(NotificationPreference notificationPreference) {
        User user = currentUser(); user.setNotificationPreference(notificationPreference); userRepository.save(user);
    }

    private User currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) throw new AccessDeniedException("Unauthorized access");
        return userRepository.findByEmail(jwt.getClaim("email")).orElseThrow(() -> new AccessDeniedException("User not found"));
    }

    private void requireAdminForManagerRole(Role role) {
        if (role != Role.MANAGER) return;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)
                || !jwt.getClaimAsStringList("roles").contains(Role.ADMIN.name())) {
            throw new AccessDeniedException("Only an administrator can grant or revoke the manager role");
        }
    }

    private int operationalRoleCount(User user) {
        if (user.getUserRoles() == null) return 0;
        return (int) user.getUserRoles().stream().filter(userRole -> OPERATIONAL_ROLES.contains(userRole.getRole())).count();
    }

    private void rejectAdminMutation(Role role) {
        if (role == Role.ADMIN) throw new IllegalArgumentException("ADMIN role cannot be changed through the API");
    }
}
