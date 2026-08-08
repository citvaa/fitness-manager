package com.example.demo.service.impl.user;

import com.example.demo.config.core.AppConfig;
import com.example.demo.enums.Role;
import com.example.demo.mapper.user.UserMapper;
import com.example.demo.model.user.User;
import com.example.demo.model.user.UserRole;
import com.example.demo.repository.PaymentRepository;
import com.example.demo.repository.user.ClientRepository;
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
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

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
    private PaymentRepository paymentRepository;
    @Mock
    private EmailService emailService;

    private UserServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new UserServiceImpl(userRepository, userMapper, passwordEncoder, appConfig, jwtUtil,
                userRoleRepository, trainerRepository, clientRepository, paymentRepository, emailService);
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
    void requestPasswordReset_setsResetKeyAndSendsEmailWhenUserExists() {
        User user = User.builder().id(1).email("a@gym.com").build();
        when(appConfig.getResetKeyValidityMinutes()).thenReturn(30);
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
        User user = User.builder().id(1).userRoles(new HashSet<>()).build();
        when(userRepository.findById(1)).thenReturn(Optional.of(user));

        service.addRole(1, Role.MANAGER);

        verify(userRoleRepository).save(any(UserRole.class));
        assertThat(user.getUserRoles()).hasSize(1);
        assertThat(user.getUserRoles().iterator().next().getRole()).isEqualTo(Role.MANAGER);
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
}
