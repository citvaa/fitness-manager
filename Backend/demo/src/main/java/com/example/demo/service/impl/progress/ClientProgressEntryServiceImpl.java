package com.example.demo.service.impl.progress;

import com.example.demo.config.cache.RedisConfig;
import com.example.demo.dto.progress.ClientProgressEntryDTO;
import com.example.demo.mapper.progress.ClientProgressEntryMapper;
import com.example.demo.model.progress.ClientProgressEntry;
import com.example.demo.model.user.Client;
import com.example.demo.repository.progress.ClientProgressEntryRepository;
import com.example.demo.repository.user.ClientRepository;
import com.example.demo.security.TrainerClientAccessGuard;
import com.example.demo.service.params.request.progress.CreateProgressEntryRequest;
import com.example.demo.service.progress.ClientProgressEntryService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClientProgressEntryServiceImpl implements ClientProgressEntryService {

    private final ClientProgressEntryRepository clientProgressEntryRepository;
    private final ClientRepository clientRepository;
    private final ClientProgressEntryMapper clientProgressEntryMapper;
    private final TrainerClientAccessGuard trainerClientAccessGuard;

    @Override
    @Transactional
    // Invalidate the cached AI progress narrative for this client - it was generated from the
    // measurement history that just changed. See AGENTS.md ("Upgrade: service layer decisions").
    @CacheEvict(value = RedisConfig.CLIENT_PROGRESS_INSIGHT_CACHE, key = "#request.clientId")
    public ClientProgressEntryDTO create(CreateProgressEntryRequest request) {
        // A trainer may only record progress for a client they have actually trained - see
        // AGENTS.md ("Upgrade: service layer decisions"). No-op for MANAGER.
        trainerClientAccessGuard.assertCanAccessClient(request.getClientId());

        Client client = clientRepository.findById(request.getClientId())
                .orElseThrow(() -> new EntityNotFoundException("Client not found"));

        ClientProgressEntry entry = ClientProgressEntry.builder()
                .client(client)
                .entryDate(request.getEntryDate())
                .weightKg(request.getWeightKg())
                .bodyFatPercent(request.getBodyFatPercent())
                .waistCm(request.getWaistCm())
                .chestCm(request.getChestCm())
                .hipCm(request.getHipCm())
                .thighCm(request.getThighCm())
                .armCm(request.getArmCm())
                .notes(request.getNotes())
                .build();

        return clientProgressEntryMapper.toDto(clientProgressEntryRepository.save(entry));
    }

    @Override
    public List<ClientProgressEntryDTO> getForClient(Integer clientId) {
        // A trainer may only view progress for a client they have actually trained - see
        // AGENTS.md ("Upgrade: service layer decisions"). No-op for MANAGER. Not applied to
        // getMine() below, which resolves the client from the caller's own JWT and reads the
        // repository directly - a CLIENT caller here would otherwise be misread as a TRAINER
        // and rejected for "never trained this client".
        trainerClientAccessGuard.assertCanAccessClient(clientId);
        return clientProgressEntryMapper.toDto(clientProgressEntryRepository.findByClientIdOrderByEntryDateAsc(clientId));
    }

    @Override
    public List<ClientProgressEntryDTO> getMine() {
        Client client = getAuthenticatedClient();
        return clientProgressEntryMapper.toDto(clientProgressEntryRepository.findByClientIdOrderByEntryDateAsc(client.getId()));
    }

    private @NotNull Client getAuthenticatedClient() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new AccessDeniedException("Unauthorized access!");
        }

        String email = jwt.getClaim("email");

        return clientRepository.findByUserEmail(email)
                .orElseThrow(() -> new RuntimeException("Client not found for the logged-in user!"));
    }
}
