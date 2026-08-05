package com.example.demo.security;

import com.example.demo.model.user.Trainer;
import com.example.demo.repository.user.ClientAppointmentRepository;
import com.example.demo.repository.user.TrainerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Scopes TRAINER access to client progress data (see AGENTS.md - "Upgrade: service layer
 * decisions"). A trainer may only view/record progress for clients they have actually trained -
 * derived from shared {@code Appointment}/{@code ClientAppointment} history, the same way a
 * trainer is already linked to a client everywhere else in this codebase, rather than via a
 * separate trainer-client assignment table. MANAGER is exempt (can access any client).
 * <p>
 * A concrete {@code @Component} rather than a service interface + impl, matching the existing
 * {@code JwtUtil}/{@code JsonUtil} pattern for cross-cutting infrastructure helpers that aren't
 * themselves a business-domain service.
 */
@Component
@RequiredArgsConstructor
public class TrainerClientAccessGuard {

    private final TrainerRepository trainerRepository;
    private final ClientAppointmentRepository clientAppointmentRepository;

    /**
     * @throws AccessDeniedException if the caller is not authenticated, or is a TRAINER who has
     *                                never trained the given client. No-op for MANAGER.
     */
    public void assertCanAccessClient(Integer clientId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new AccessDeniedException("Unauthorized access!");
        }

        List<String> roles = jwt.getClaimAsStringList("roles");
        if (roles != null && roles.contains("MANAGER")) {
            return;
        }

        String email = jwt.getClaim("email");
        Trainer trainer = trainerRepository.findByUserEmail(email)
                .orElseThrow(() -> new AccessDeniedException("Trainer not found for the logged-in user!"));

        if (!clientAppointmentRepository.existsByClientIdAndAppointmentTrainerId(clientId, trainer.getId())) {
            throw new AccessDeniedException("You have never trained this client - access denied");
        }
    }
}
