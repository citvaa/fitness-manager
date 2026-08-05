package com.example.demo.service.impl.progress;

import com.example.demo.config.cache.RedisConfig;
import com.example.demo.dto.progress.ClientProgressInsightDTO;
import com.example.demo.model.progress.ClientPersonalRecord;
import com.example.demo.model.progress.ClientProgressEntry;
import com.example.demo.model.user.Client;
import com.example.demo.repository.progress.ClientPersonalRecordRepository;
import com.example.demo.repository.progress.ClientProgressEntryRepository;
import com.example.demo.repository.user.ClientRepository;
import com.example.demo.security.TrainerClientAccessGuard;
import com.example.demo.service.ai.ClaudeInsightService;
import com.example.demo.service.progress.ClientProgressInsightService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Generates a short narrative summary + recommendation from a client's progress-entry and
 * personal-record history via Claude. Cached manually (rather than via {@code @Cacheable}) via
 * the shared {@link #getOrGenerateSummary(Integer)} helper, which both {@link #getSummary(Integer)}
 * (TRAINER/MANAGER, ownership-checked) and {@link #getMySummary()} (CLIENT, no ownership check
 * needed - it resolves the client from the caller's own JWT) call directly as a plain Java
 * method - avoiding both a Spring AOP self-invocation caching bug (calling an {@code @Cacheable}
 * method from within the same bean silently bypasses the annotation) and, separately,
 * misapplying the TRAINER-ownership check to a CLIENT caller. Cache invalidation on new entries
 * is explicit, via {@code ClientProgressEntryServiceImpl}'s {@code @CacheEvict} - see AGENTS.md
 * ("Upgrade: service layer decisions").
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClientProgressInsightServiceImpl implements ClientProgressInsightService {

    private static final String SYSTEM_PROMPT = """
            You are a supportive fitness coach's assistant. You are given a client's body-
            measurement history and personal exercise records, already computed. Write a short
            (3-5 sentence) narrative summary of their progress and one concrete, encouraging
            recommendation for what to focus on next. Do not invent numbers beyond what is given.
            No markdown, plain prose only.
            """;

    private final ClientRepository clientRepository;
    private final ClientProgressEntryRepository clientProgressEntryRepository;
    private final ClientPersonalRecordRepository clientPersonalRecordRepository;
    private final ClaudeInsightService claudeInsightService;
    private final CacheManager cacheManager;
    private final TrainerClientAccessGuard trainerClientAccessGuard;

    @Override
    public ClientProgressInsightDTO getSummary(Integer clientId) {
        // A trainer may only view the narrative for a client they have actually trained - see
        // AGENTS.md ("Upgrade: service layer decisions"). No-op for MANAGER. Not applied to
        // getMySummary() below, which resolves the client from the caller's own JWT and shares
        // the cache-lookup/generate/put logic directly rather than calling this method - a
        // CLIENT caller here would otherwise be misread as a TRAINER and rejected.
        trainerClientAccessGuard.assertCanAccessClient(clientId);
        return getOrGenerateSummary(clientId);
    }

    @Override
    public ClientProgressInsightDTO getMySummary() {
        Client client = getAuthenticatedClient();
        return getOrGenerateSummary(client.getId());
    }

    private ClientProgressInsightDTO getOrGenerateSummary(Integer clientId) {
        Cache cache = cacheManager.getCache(RedisConfig.CLIENT_PROGRESS_INSIGHT_CACHE);
        if (cache != null) {
            ClientProgressInsightDTO cached = cache.get(clientId, ClientProgressInsightDTO.class);
            if (cached != null) {
                return cached;
            }
        }

        ClientProgressInsightDTO fresh = generateSummary(clientId);
        if (cache != null) {
            cache.put(clientId, fresh);
        }
        return fresh;
    }

    private ClientProgressInsightDTO generateSummary(Integer clientId) {
        if (!clientRepository.existsById(clientId)) {
            throw new EntityNotFoundException("Client not found");
        }

        List<ClientProgressEntry> entries = clientProgressEntryRepository.findByClientIdOrderByEntryDateAsc(clientId);
        List<ClientPersonalRecord> records = clientPersonalRecordRepository.findByClientIdOrderByRecordDateDesc(clientId);

        String data = buildDataSummary(entries, records);
        String narrative = claudeInsightService.generate(SYSTEM_PROMPT, data);

        ClientProgressInsightDTO dto = new ClientProgressInsightDTO();
        dto.setClientId(clientId);
        dto.setNarrative(narrative);
        dto.setGeneratedAt(LocalDateTime.now());
        return dto;
    }

    private String buildDataSummary(List<ClientProgressEntry> entries, List<ClientPersonalRecord> records) {
        StringBuilder sb = new StringBuilder();

        sb.append("Body measurement entries (oldest to newest), one per line:\n");
        if (entries.isEmpty()) {
            sb.append("  (none recorded yet)\n");
        } else {
            for (ClientProgressEntry entry : entries) {
                sb.append("  - ").append(entry.getEntryDate()).append(": ");
                appendIfPresent(sb, "weight", entry.getWeightKg(), "kg");
                appendIfPresent(sb, "body fat", entry.getBodyFatPercent(), "%");
                appendIfPresent(sb, "waist", entry.getWaistCm(), "cm");
                appendIfPresent(sb, "chest", entry.getChestCm(), "cm");
                appendIfPresent(sb, "hip", entry.getHipCm(), "cm");
                appendIfPresent(sb, "thigh", entry.getThighCm(), "cm");
                appendIfPresent(sb, "arm", entry.getArmCm(), "cm");
                if (entry.getNotes() != null && !entry.getNotes().isBlank()) {
                    sb.append("notes=\"").append(entry.getNotes()).append("\" ");
                }
                sb.append("\n");
            }
        }

        sb.append("Personal records (most recent first), one per line:\n");
        if (records.isEmpty()) {
            sb.append("  (none recorded yet)\n");
        } else {
            for (ClientPersonalRecord record : records) {
                sb.append("  - ").append(record.getRecordDate()).append(": ")
                        .append(record.getExerciseName()).append(" = ")
                        .append(record.getValue()).append(" ").append(record.getUnit())
                        .append("\n");
            }
        }

        return sb.toString();
    }

    private void appendIfPresent(StringBuilder sb, String label, Object value, String unit) {
        if (value != null) {
            sb.append(label).append("=").append(value).append(unit).append(" ");
        }
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
