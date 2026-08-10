package com.example.demo.service.impl.user;

import com.example.demo.config.core.AppConfig;
import com.example.demo.dto.user.UserDTO;
import com.example.demo.enums.NotificationPreference;
import com.example.demo.enums.Role;
import com.example.demo.mapper.user.UserMapper;
import com.example.demo.model.user.User;
import com.example.demo.model.user.UserRole;
import com.example.demo.repository.gym.RoomCheckInRepository;
import com.example.demo.repository.progress.ClientPersonalRecordRepository;
import com.example.demo.repository.progress.ClientProgressEntryRepository;
import com.example.demo.repository.schedule.TrainerScheduleRepository;
import com.example.demo.repository.user.ClientAppointmentRepository;
import com.example.demo.repository.user.ClientRepository;
import com.example.demo.repository.user.ClientSessionTrackingRepository;
import com.example.demo.repository.user.TrainerRepository;
import com.example.demo.repository.user.UserRepository;
import com.example.demo.repository.user.UserRoleRepository;
import com.example.demo.service.notification.email.EmailService;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.UUID;

@Transactional(readOnly = true)
@RequiredArgsConstructor
@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final AppConfig appConfig;
    private final JwtUtil jwtUtil;
    private final UserRoleRepository userRoleRepository;
    private final TrainerRepository trainerRepository;
    private final ClientRepository clientRepository;
    private final ClientSessionTrackingRepository clientSessionTrackingRepository;
    private final ClientAppointmentRepository clientAppointmentRepository;
    private final RoomCheckInRepository roomCheckInRepository;
    private final ClientProgressEntryRepository clientProgressEntryRepository;
    private final ClientPersonalRecordRepository clientPersonalRecordRepository;
    private final PaymentRepository paymentRepository;
    private final AppointmentRepository appointmentRepository;
    private final TrainerScheduleRepository trainerScheduleRepository;
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
                .orElseThrow(() -> new EntityNotFoundException("Korisnik nije pronađen")));
    }

    @Transactional
    public UserDTO create(@NotNull CreateUserRequest request) {
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

        ActivationEmailData emailData = ActivationEmailData.builder()
                .registrationKey(registration_key)
                .registrationKeyValidity(DateTimeUtil.formatTime(registration_key_validity))
                .frontendUrl(appConfig.getFrontend().getUrl())
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
                }).orElseThrow(() -> new EntityNotFoundException("Korisnik nije pronađen"));
    }

    @Transactional
    public void delete(Integer id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Korisnik nije pronađen"));

        // Every table with a client_id FK (payments, clientSessionTrackings, clientAppointments,
        // roomCheckIns, progress entries, personal records) is cleared via explicit bulk JPQL
        // deletes before the Client row itself goes. The original bug here was that only
        // payments were ever cleared, leaving the Client row blocked by an FK violation the
        // frontend silently swallowed. Deliberately NOT done via Client's cascade=ALL/
        // orphanRemoval collections + clientRepository.delete(client) (entity-level delete) -
        // that route hits a second, deeper pre-existing bug (see AGENTS.md "Known issues" -
        // BaseEntity's id-less equals()/hashCode() corrupts Hibernate's Set handling once an
        // Appointment's own bidirectional clientAppointments collection gets pulled into the
        // same cascade graph, e.g. any shared GROUP session), throwing a
        // TransientObjectException. Bulk JPQL deletes never touch Java object equality, so they
        // sidestep that bug entirely - see AGENTS.md "Upgrade: manager-testing fixes".
        clientRepository.findByUserEmail(user.getEmail()).ifPresent(client -> {
            clientSessionTrackingRepository.deleteByClient(client);
            clientAppointmentRepository.deleteByClient(client);
            roomCheckInRepository.deleteByClient(client);
            clientProgressEntryRepository.deleteByClient(client);
            clientPersonalRecordRepository.deleteByClient(client);
        });
        paymentRepository.deleteByUser(user);
        clientRepository.deleteByUser(user);

        // Trainer has no such cascade collections - its TrainerSchedule rows and any Appointment
        // it's assigned to must be detached manually before the Trainer row itself can go.
        trainerRepository.findByUserEmail(user.getEmail()).ifPresent(trainer -> {
            appointmentRepository.clearTrainer(trainer);
            trainerScheduleRepository.deleteByTrainer(trainer);
            trainerRepository.delete(trainer);
        });

        // Both the UserRole rows and the User row itself go via bulk JPQL deletes
        // (userRepository.deleteUser(), not the inherited entity-level delete(User)) for the
        // same reason as the Client cleanup above: User.userRoles is EAGER-fetched with
        // cascade=ALL/orphanRemoval=true, so findById() already loaded it into the persistence
        // context, and an entity-level delete(user) cascading over that collection hits the
        // pre-existing BaseEntity id-less equals()/hashCode() bug - this is the real bug behind
        // the silently-failing "Obriši" button (see AGENTS.md "Upgrade: manager-testing fixes")
        // and "Known issues" - deleting a user with any role at all hit this, not just one with
        // a Client/Trainer profile.
        userRoleRepository.deleteByUser(user);
        userRepository.deleteUser(user);
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

    public AuthResponse login(@NotNull LoginUserRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new EntityNotFoundException("Korisnik nije pronađen"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("Pogrešna lozinka");
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
            throw new BadCredentialsException("Refresh token je istekao, prijavite se ponovo");
        }

        Claims claims = jwtUtil.parseToken(refreshToken);
        String userId = claims.getSubject();
        User user = userRepository.findById(Integer.parseInt(userId))
                .orElseThrow(() -> new EntityNotFoundException("Korisnik nije pronađen"));

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
                    .frontendUrl(appConfig.getFrontend().getUrl())
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
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Korisnik nije pronađen"));

        boolean alreadyHasRole = user.getUserRoles() != null
                && user.getUserRoles().stream().anyMatch(userRole -> userRole.getRole().equals(role));

        if (alreadyHasRole) {
            throw new IllegalArgumentException("Korisnik već ima rolu " + role);
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
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Korisnik nije pronađen"));

        // A MANAGER must not be able to strip their own MANAGER role - that would lock them out
        // of the admin area with no other MANAGER account able to undo it from the UI. Checked
        // here (not just the frontend button) since this is also reachable directly via the API.
        if (role == Role.MANAGER && isCurrentlyAuthenticatedUser(user)) {
            throw new AccessDeniedException("Ne možete sami sebi oduzeti MANAGER rolu.");
        }

        Optional<UserRole> userRoleToRemove = user.getUserRoles().stream().filter(userRole -> userRole.getRole().equals(role)).findFirst();

        if (userRoleToRemove.isEmpty()) {
            throw new IllegalArgumentException("Korisnik nema rolu " + role);
        }

        userRoleRepository.delete(userRoleToRemove.get());
        user.getUserRoles().remove(userRoleToRemove.get());

        userRepository.save(user);
    }

    /** Same JWT->email idiom used across the codebase (see AGENTS.md, "Upgrade: service layer
     * decisions") - returns false rather than throwing when there's no authenticated caller, so
     * callers without a request context (e.g. TrainerServiceImpl.delete() removing a TRAINER
     * role) are unaffected. */
    private boolean isCurrentlyAuthenticatedUser(User user) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            return false;
        }
        String email = jwt.getClaim("email");
        return email != null && email.equals(user.getEmail());
    }

    @Transactional
    public User findOrCreateUser(@NotNull CreateUserRequest request) {
        return userRepository.findByEmail(request.getEmail())
                .orElseGet(() -> userMapper.toEntity(create(request)));
    }

    @Transactional
    public void updateNotificationPreference(Integer id, NotificationPreference notificationPreference) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Korisnik nije pronađen"));

        user.setNotificationPreference(notificationPreference);
        userRepository.save(user);
    }
}
