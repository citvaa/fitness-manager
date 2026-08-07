package com.example.demo.security;

import com.example.demo.model.user.Trainer;
import com.example.demo.repository.user.ClientAppointmentRepository;
import com.example.demo.repository.user.TrainerRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link TrainerClientAccessGuard} - see AGENTS.md ("Upgrade: service layer
 * decisions"): a TRAINER may only access a client they have actually trained (derived from
 * shared Appointment/ClientAppointment history); MANAGER is exempt from the check entirely.
 */
@ExtendWith(MockitoExtension.class)
class TrainerClientAccessGuardTest {

    @Mock
    private TrainerRepository trainerRepository;
    @Mock
    private ClientAppointmentRepository clientAppointmentRepository;

    private TrainerClientAccessGuard guard;

    @BeforeEach
    void setUp() {
        guard = new TrainerClientAccessGuard(trainerRepository, clientAppointmentRepository);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private Jwt jwtWithEmailAndRoles(String email, List<String> roles) {
        return Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("email", email)
                .claim("roles", roles)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(900))
                .build();
    }

    private void authenticateAs(Jwt jwt) {
        Authentication authentication = new UsernamePasswordAuthenticationToken(jwt, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @Test
    void assertCanAccessClient_throwsWhenUnauthenticated() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(() -> guard.assertCanAccessClient(1))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void assertCanAccessClient_throwsWhenPrincipalIsNotAJwt() {
        Authentication authentication = new UsernamePasswordAuthenticationToken("not-a-jwt", null, List.of());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        assertThatThrownBy(() -> guard.assertCanAccessClient(1))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void assertCanAccessClient_managerIsExemptRegardlessOfClient() {
        authenticateAs(jwtWithEmailAndRoles("manager@gym.com", List.of("MANAGER")));

        assertThatNoException().isThrownBy(() -> guard.assertCanAccessClient(999));

        verifyNoInteractions(trainerRepository, clientAppointmentRepository);
    }

    @Test
    void assertCanAccessClient_managerWithMultipleRolesIsStillExempt() {
        authenticateAs(jwtWithEmailAndRoles("mgr-trainer@gym.com", List.of("TRAINER", "MANAGER")));

        assertThatNoException().isThrownBy(() -> guard.assertCanAccessClient(5));

        verifyNoInteractions(trainerRepository, clientAppointmentRepository);
    }

    @Test
    void assertCanAccessClient_trainerWhoHasTrainedTheClientIsAllowed() {
        Trainer trainer = Trainer.builder().id(10).build();
        authenticateAs(jwtWithEmailAndRoles("trainer@gym.com", List.of("TRAINER")));
        when(trainerRepository.findByUserEmail("trainer@gym.com")).thenReturn(Optional.of(trainer));
        when(clientAppointmentRepository.existsByClientIdAndAppointmentTrainerId(7, 10)).thenReturn(true);

        assertThatNoException().isThrownBy(() -> guard.assertCanAccessClient(7));
    }

    @Test
    void assertCanAccessClient_trainerWhoHasNeverTrainedTheClientIsRejected() {
        Trainer trainer = Trainer.builder().id(10).build();
        authenticateAs(jwtWithEmailAndRoles("trainer@gym.com", List.of("TRAINER")));
        when(trainerRepository.findByUserEmail("trainer@gym.com")).thenReturn(Optional.of(trainer));
        when(clientAppointmentRepository.existsByClientIdAndAppointmentTrainerId(7, 10)).thenReturn(false);

        assertThatThrownBy(() -> guard.assertCanAccessClient(7))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("never trained this client");
    }

    @Test
    void assertCanAccessClient_throwsWhenNoTrainerRowForAuthenticatedEmail() {
        authenticateAs(jwtWithEmailAndRoles("ghost@gym.com", List.of("TRAINER")));
        when(trainerRepository.findByUserEmail("ghost@gym.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> guard.assertCanAccessClient(1))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Trainer not found");

        verifyNoInteractions(clientAppointmentRepository);
    }
}
