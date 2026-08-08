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
import org.springframework.cache.CacheManager;
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
    private final CacheManager cacheManager;

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
    @Transactional
    public ClientProgressEntryDTO update(Integer id, CreateProgressEntryRequest request) {
        ClientProgressEntry entry = clientProgressEntryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Progress entry not found"));

        // Authorization is checked against the entry's own (immutable) client, not the request
        // body - the request's clientId is otherwise ignored for update, so a caller can't
        // reassign someone else's entry to a client they can access. See AGENTS.md
        // ("Upgrade: Faza 9 decisions").
        trainerClientAccessGuard.assertCanAccessClient(entry.getClient().getId());

        entry.setEntryDate(request.getEntryDate());
        entry.setWeightKg(request.getWeightKg());
        entry.setBodyFatPercent(request.getBodyFatPercent());
        entry.setWaistCm(request.getWaistCm());
        entry.setChestCm(request.getChestCm());
        entry.setHipCm(request.getHipCm());
        entry.setThighCm(request.getThighCm());
        entry.setArmCm(request.getArmCm());
        entry.setNotes(request.getNotes());

        ClientProgressEntryDTO dto = clientProgressEntryMapper.toDto(clientProgressEntryRepository.save(entry));
        evictInsightCache(entry.getClient().getId());
        return dto;
    }

    @Override
    @Transactional
    public void delete(Integer id) {
        ClientProgressEntry entry = clientProgressEntryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Progress entry not found"));

        trainerClientAccessGuard.assertCanAccessClient(entry.getClient().getId());

        Integer clientId = entry.getClient().getId();
        clientProgressEntryRepository.delete(entry);
        evictInsightCache(clientId);
    }

    // update()/delete() can't use a declarative @CacheEvict(key = "#request.clientId") like
    // create() does - the client isn't known from the method arguments alone (an id, not a
    // clientId), only after the entry is fetched - so the cache is evicted manually here instead,
    // the same "no self-invocation, no annotation needed" style CacheManager access
    // ClientProgressInsightServiceImpl already uses. See AGENTS.md ("Upgrade: Faza 9 decisions").
    private void evictInsightCache(Integer clientId) {
        var cache = cacheManager.getCache(RedisConfig.CLIENT_PROGRESS_INSIGHT_CACHE);
        if (cache != null) {
            cache.evict(clientId);
        }
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
