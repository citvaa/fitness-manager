package com.example.demo.service.impl.user;

import com.example.demo.config.core.AppConfig;
import com.example.demo.enums.Role;
import com.example.demo.mapper.user.UserMapper;
import com.example.demo.model.user.Client;
import com.example.demo.model.user.User;
import com.example.demo.model.user.UserRole;
import com.example.demo.repository.AppointmentRepository;
import com.example.demo.repository.PaymentRepository;
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
import com.example.demo.service.params.request.user.LoginUserRequest;
import com.example.demo.service.params.request.user.RegisterUserRequest;
import com.example.demo.service.params.request.user.ResetPasswordRequest;
import com.example.demo.service.params.response.user.AuthResponse;
import com.example.demo.util.JwtUtil;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link UserServiceImpl} - login/register/forgot-password/reset-password/
 * add-remove-role. This is the most security-sensitive code in the app (password reset, role
 * grants) and, until Faza 9, had zero test coverage of its own - see AGENTS.md
 * ("Upgrade: Faza 9 decisions").
 */
@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserMapper userMapper;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AppConfig appConfig;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private UserRoleRepository userRoleRepository;
    @Mock
    private TrainerRepository trainerRepository;
    @Mock
    private ClientRepository clientRepository;
    @Mock
    private ClientSessionTrackingRepository clientSessionTrackingRepository;
    @Mock
    private ClientAppointmentRepository clientAppointmentRepository;
    @Mock
    private RoomCheckInRepository roomCheckInRepository;
    @Mock
    private ClientProgressEntryRepository clientProgressEntryRepository;
    @Mock
    private ClientPersonalRecordRepository clientPersonalRecordRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private AppointmentRepository appointmentRepository;
    @Mock
    private TrainerScheduleRepository trainerScheduleRepository;
    @Mock
    private EmailService emailService;

    private UserServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new UserServiceImpl(userRepository, userMapper, passwordEncoder, appConfig, jwtUtil,
                userRoleRepository, trainerRepository, clientRepository, clientSessionTrackingRepository,
                clientAppointmentRepository, roomCheckInRepository, clientProgressEntryRepository,
                clientPersonalRecordRepository, paymentRepository, appointmentRepository,
                trainerScheduleRepository, emailService);
    }

    @Test
    void login_returnsTokensOnCorrectPassword() {
        User user = User.builder().id(1).email("a@gym.com").password("hashed").userRoles(new HashSet<>()).build();
        when(userRepository.findByEmail("a@gym.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("plain", "hashed")).thenReturn(true);
        when(jwtUtil.generateAccessToken(user)).thenReturn("access-token");
        when(jwtUtil.generateRefreshToken(user)).thenReturn("refresh-token");
        when(jwtUtil.getTokenExpirationTime("access-token")).thenReturn(LocalDateTime.now().plusMinutes(15));
        when(jwtUtil.getTokenExpirationTime("refresh-token")).thenReturn(LocalDateTime.now().plusHours(2));

        AuthResponse response = service.login(new LoginUserRequest("a@gym.com", "plain"));

        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
    }

    @Test
    void login_throwsBadCredentialsOnWrongPassword() {
        User user = User.builder().id(1).email("a@gym.com").password("hashed").build();
        when(userRepository.findByEmail("a@gym.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> service.login(new LoginUserRequest("a@gym.com", "wrong")))
                .isInstanceOf(BadCredentialsException.class);

        verify(jwtUtil, never()).generateAccessToken(any());
    }

    @Test
    void login_throwsNotFoundForUnknownEmail() {
        when(userRepository.findByEmail("nobody@gym.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.login(new LoginUserRequest("nobody@gym.com", "x")))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void register_activatesUserWhenKeyIsValid() {
        User user = User.builder().id(1).email("a@gym.com")
                .registrationKey("valid-key")
                .registrationKeyValidity(LocalDateTime.now().plusMinutes(5))
                .isActivated(false)
                .build();
        when(userRepository.findByRegistrationKey("valid-key")).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("newpass")).thenReturn("hashed-newpass");

        service.register(new RegisterUserRequest("valid-key", "newpass"));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getPassword()).isEqualTo("hashed-newpass");
        assertThat(saved.getRegistrationKey()).isNull();
        assertThat(saved.getRegistrationKeyValidity()).isNull();
        assertThat(saved.getIsActivated()).isTrue();
    }

    @Test
    void register_isANoOpWhenKeyHasExpired() {
        User user = User.builder().id(1).email("a@gym.com")
                .registrationKey("expired-key")
                .registrationKeyValidity(LocalDateTime.now().minusMinutes(1))
                .isActivated(false)
                .build();
        when(userRepository.findByRegistrationKey("expired-key")).thenReturn(Optional.of(user));

        service.register(new RegisterUserRequest("expired-key", "newpass"));

        verify(userRepository, never()).save(any());
    }

    @Test
    void delete_clearsClientDependentsBeforeBulkDeletingClientAndUser() {
        // Regression test for the "Obriši" (delete) bug - see AGENTS.md "Upgrade: manager-testing
        // fixes". A client's clientSessionTrackings/clientAppointments must be cleared via bulk
        // JPQL delete (deleteByClient) before the Client/User rows themselves go, or the DB FK
        // constraint (or, when attempted via JPA cascade instead, a deeper pre-existing
        // BaseEntity equals()/hashCode() bug) blocks the whole delete.
        User user = User.builder().id(1).email("client@gym.com").build();
        Client client = Client.builder().id(9).user(user).build();
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(clientRepository.findByUserEmail("client@gym.com")).thenReturn(Optional.of(client));
        when(trainerRepository.findByUserEmail("client@gym.com")).thenReturn(Optional.empty());

        service.delete(1);

        verify(clientSessionTrackingRepository).deleteByClient(client);
        verify(clientAppointmentRepository).deleteByClient(client);
        verify(roomCheckInRepository).deleteByClient(client);
        verify(clientProgressEntryRepository).deleteByClient(client);
        verify(clientPersonalRecordRepository).deleteByClient(client);
        verify(paymentRepository).deleteByUser(user);
        verify(clientRepository).deleteByUser(user);
        verify(userRoleRepository).deleteByUser(user);
        verify(userRepository).deleteUser(user);
        // The entity-level delete(User) must stay unused - it cascades over the eagerly-loaded
        // userRoles collection and hits the equals()/hashCode() bug this fix works around.
        verify(userRepository, never()).delete(any());
    }

    @Test
    void delete_detachesTrainerFromAppointmentsBeforeDeletingTrainer() {
        User user = User.builder().id(2).email("trainer@gym.com").build();
        com.example.demo.model.user.Trainer trainer =
                com.example.demo.model.user.Trainer.builder().id(5).user(user).build();
        when(userRepository.findById(2)).thenReturn(Optional.of(user));
        when(clientRepository.findByUserEmail("trainer@gym.com")).thenReturn(Optional.empty());
        when(trainerRepository.findByUserEmail("trainer@gym.com")).thenReturn(Optional.of(trainer));

        service.delete(2);

        verify(appointmentRepository).clearTrainer(trainer);
        verify(trainerScheduleRepository).deleteByTrainer(trainer);
        verify(trainerRepository).delete(trainer);
        verify(userRepository).deleteUser(user);
    }

    @Test
    void requestPasswordReset_setsResetKeyAndSendsEmailWhenUserExists() {
        User user = User.builder().id(1).email("a@gym.com").build();
        when(appConfig.getResetKeyValidityMinutes()).thenReturn(30);
        when(appConfig.getFrontend()).thenReturn(new AppConfig.Frontend());
        when(userRepository.findByEmail("a@gym.com")).thenReturn(Optional.of(user));

        service.requestPasswordReset("a@gym.com");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getResetKey()).isNotBlank();
        verify(emailService).sendResetPasswordEmail(eq("a@gym.com"), any());
    }

    @Test
    void requestPasswordReset_isANoOpForUnknownEmail() {
        when(userRepository.findByEmail("nobody@gym.com")).thenReturn(Optional.empty());

        service.requestPasswordReset("nobody@gym.com");

        verify(userRepository, never()).save(any());
        verify(emailService, never()).sendResetPasswordEmail(anyString(), any());
    }

    @Test
    void resetPassword_updatesPasswordAndClearsResetKey() {
        User user = User.builder().id(1).email("a@gym.com").resetKey("reset-key")
                .resetKeyValidity(LocalDateTime.now().plusMinutes(10)).build();
        when(userRepository.findByResetKey("reset-key")).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("newpass")).thenReturn("hashed-newpass");

        service.resetPassword(new ResetPasswordRequest("reset-key", "newpass"));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getPassword()).isEqualTo("hashed-newpass");
        assertThat(captor.getValue().getResetKey()).isNull();
        assertThat(captor.getValue().getResetKeyValidity()).isNull();
    }

    @Test
    void addRole_addsNewRoleToUser() {
        // TRAINER/CLIENT roles have no ADMIN gate - only MANAGER does (see the two tests below).
        User user = User.builder().id(1).userRoles(new HashSet<>()).build();
        when(userRepository.findById(1)).thenReturn(Optional.of(user));

        service.addRole(1, Role.TRAINER);

        verify(userRoleRepository).save(any(UserRole.class));
        assertThat(user.getUserRoles()).hasSize(1);
        assertThat(user.getUserRoles().iterator().next().getRole()).isEqualTo(Role.TRAINER);
    }

    @Test
    void addRole_rejectsNonAdminGrantingManagerRole() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("email", "manager@gym.com")
                .claim("roles", java.util.List.of("MANAGER"))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .build();
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(jwt, null));

        try {
            assertThatThrownBy(() -> service.addRole(1, Role.MANAGER))
                    .isInstanceOf(AccessDeniedException.class);

            verify(userRepository, never()).findById(any());
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void addRole_allowsAdminGrantingManagerRole() {
        User user = User.builder().id(1).userRoles(new HashSet<>()).build();
        when(userRepository.findById(1)).thenReturn(Optional.of(user));

        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("email", "admin@gym.com")
                .claim("roles", java.util.List.of("ADMIN", "MANAGER"))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .build();
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(jwt, null));

        try {
            service.addRole(1, Role.MANAGER);

            verify(userRoleRepository).save(any(UserRole.class));
            assertThat(user.getUserRoles()).hasSize(1);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void addRole_rejectsDuplicateRole() {
        UserRole existing = new UserRole();
        existing.setRole(Role.TRAINER);
        User user = User.builder().id(1).userRoles(new HashSet<>(java.util.List.of(existing))).build();
        when(userRepository.findById(1)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.addRole(1, Role.TRAINER))
                .isInstanceOf(IllegalArgumentException.class);

        verify(userRoleRepository, never()).save(any());
    }

    @Test
    void removeRole_removesExistingRole() {
        UserRole existing = new UserRole();
        existing.setRole(Role.TRAINER);
        User user = User.builder().id(1).userRoles(new HashSet<>(java.util.List.of(existing))).build();
        when(userRepository.findById(1)).thenReturn(Optional.of(user));

        service.removeRole(1, Role.TRAINER);

        verify(userRoleRepository).delete(existing);
        assertThat(user.getUserRoles()).isEmpty();
    }

    @Test
    void removeRole_rejectsWhenUserDoesNotHaveRole() {
        User user = User.builder().id(1).userRoles(new HashSet<>()).build();
        when(userRepository.findById(1)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.removeRole(1, Role.TRAINER))
                .isInstanceOf(IllegalArgumentException.class);

        verify(userRoleRepository, never()).delete(any());
    }

    @Test
    void removeRole_rejectsManagerRemovingOwnManagerRole() {
        UserRole existing = new UserRole();
        existing.setRole(Role.MANAGER);
        User user = User.builder().id(1).email("admin@gym.com").userRoles(new HashSet<>(java.util.List.of(existing))).build();
        when(userRepository.findById(1)).thenReturn(Optional.of(user));

        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("email", "admin@gym.com")
                .claim("roles", java.util.List.of("ADMIN", "MANAGER"))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .build();
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(jwt, null));

        try {
            assertThatThrownBy(() -> service.removeRole(1, Role.MANAGER))
                    .isInstanceOf(AccessDeniedException.class);

            verify(userRoleRepository, never()).delete(any());
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void removeRole_allowsManagerRemovingAnotherUsersManagerRole() {
        UserRole existing = new UserRole();
        existing.setRole(Role.MANAGER);
        User user = User.builder().id(2).email("other@gym.com").userRoles(new HashSet<>(java.util.List.of(existing))).build();
        when(userRepository.findById(2)).thenReturn(Optional.of(user));

        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("email", "admin@gym.com")
                .claim("roles", java.util.List.of("ADMIN", "MANAGER"))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .build();
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(jwt, null));

        try {
            service.removeRole(2, Role.MANAGER);

            verify(userRoleRepository).delete(existing);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void addRole_rejectsGrantingAdminRoleEvenAsAdmin() {
        // ADMIN must never be grantable through this generic endpoint at all - not even by an
        // existing ADMIN caller. See AGENTS.md "Upgrade: ADMIN-role security hole".
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("email", "admin@gym.com")
                .claim("roles", java.util.List.of("ADMIN", "MANAGER"))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .build();
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(jwt, null));

        try {
            assertThatThrownBy(() -> service.addRole(1, Role.ADMIN))
                    .isInstanceOf(AccessDeniedException.class);

            verify(userRepository, never()).findById(any());
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void removeRole_rejectsRevokingAdminRoleEvenAsAdmin() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("email", "admin@gym.com")
                .claim("roles", java.util.List.of("ADMIN", "MANAGER"))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .build();
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(jwt, null));

        try {
            assertThatThrownBy(() -> service.removeRole(1, Role.ADMIN))
                    .isInstanceOf(AccessDeniedException.class);

            verify(userRepository, never()).findById(any());
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void removeRole_rejectsNonAdminRevokingManagerRole() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("email", "manager@gym.com")
                .claim("roles", java.util.List.of("MANAGER"))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .build();
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(jwt, null));

        try {
            assertThatThrownBy(() -> service.removeRole(2, Role.MANAGER))
                    .isInstanceOf(AccessDeniedException.class);

            verify(userRepository, never()).findById(any());
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
