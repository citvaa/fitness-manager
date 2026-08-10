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
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link ClientProgressInsightServiceImpl} - specifically the manual
 * CacheManager-based lookup/populate logic in {@code getOrGenerateSummary} (no {@code @Cacheable}
 * annotation here, precisely so {@code getMySummary()} can call it as a plain Java method without
 * hitting the Spring AOP self-invocation caching pitfall - see AGENTS.md "Upgrade: service layer
 * decisions"), the trainer-ownership guard on {@code getSummary}, and the JWT-resolved
 * {@code getMySummary} path that skips the guard.
 */
@ExtendWith(MockitoExtension.class)
class ClientProgressInsightServiceImplTest {

    @Mock
    private ClientRepository clientRepository;
    @Mock
    private ClientProgressEntryRepository clientProgressEntryRepository;
    @Mock
    private ClientPersonalRecordRepository clientPersonalRecordRepository;
    @Mock
    private ClaudeInsightService claudeInsightService;
    @Mock
    private CacheManager cacheManager;
    @Mock
    private Cache cache;
    @Mock
    private TrainerClientAccessGuard trainerClientAccessGuard;

    private ClientProgressInsightServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ClientProgressInsightServiceImpl(clientRepository, clientProgressEntryRepository,
                clientPersonalRecordRepository, claudeInsightService, cacheManager, trainerClientAccessGuard);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getSummary_returnsCachedValueWithoutCallingClaude_onCacheHit() {
        ClientProgressInsightDTO cached = new ClientProgressInsightDTO();
        cached.setClientId(5);
        cached.setNarrative("cached narrative");

        when(cacheManager.getCache(RedisConfig.CLIENT_PROGRESS_INSIGHT_CACHE)).thenReturn(cache);
        when(cache.get(5, ClientProgressInsightDTO.class)).thenReturn(cached);

        ClientProgressInsightDTO result = service.getSummary(5);

        assertThat(result).isSameAs(cached);
        verify(trainerClientAccessGuard).assertCanAccessClient(5);
        verifyNoInteractions(claudeInsightService);
        verify(cache, never()).put(anyString(), any());
        verify(cache, never()).put(eq(5), any());
    }

    @Test
    void getSummary_generatesAndPopulatesCache_onCacheMiss() {
        Client client = Client.builder().id(5).build();
        ClientProgressEntry entry = ClientProgressEntry.builder()
                .id(1).client(client).entryDate(LocalDate.now()).weightKg(new BigDecimal("70")).build();
        ClientPersonalRecord record = ClientPersonalRecord.builder()
                .id(1).client(client).exerciseName("Squat").value(new BigDecimal("120"))
                .recordDate(LocalDate.now()).build();

        when(cacheManager.getCache(RedisConfig.CLIENT_PROGRESS_INSIGHT_CACHE)).thenReturn(cache);
        when(cache.get(5, ClientProgressInsightDTO.class)).thenReturn(null);
        when(clientRepository.existsById(5)).thenReturn(true);
        when(clientProgressEntryRepository.findByClientIdOrderByEntryDateAsc(5)).thenReturn(List.of(entry));
        when(clientPersonalRecordRepository.findByClientIdOrderByRecordDateDesc(5)).thenReturn(List.of(record));
        when(claudeInsightService.generate(anyString(), anyString())).thenReturn("Fresh narrative");

        ClientProgressInsightDTO result = service.getSummary(5);

        assertThat(result.getClientId()).isEqualTo(5);
        assertThat(result.getNarrative()).isEqualTo("Fresh narrative");
        verify(cache).put(eq(5), eq(result));

        ArgumentCaptor<String> dataCaptor = ArgumentCaptor.forClass(String.class);
        verify(claudeInsightService).generate(anyString(), dataCaptor.capture());
        assertThat(dataCaptor.getValue()).contains("weight=70");
        assertThat(dataCaptor.getValue()).contains("Squat");
    }

    @Test
    void getSummary_worksWithoutThrowingWhenCacheRegionIsMissing() {
        when(cacheManager.getCache(RedisConfig.CLIENT_PROGRESS_INSIGHT_CACHE)).thenReturn(null);
        when(clientRepository.existsById(5)).thenReturn(true);
        when(clientProgressEntryRepository.findByClientIdOrderByEntryDateAsc(5)).thenReturn(List.of());
        when(clientPersonalRecordRepository.findByClientIdOrderByRecordDateDesc(5)).thenReturn(List.of());
        when(claudeInsightService.generate(anyString(), anyString())).thenReturn("narrative");

        ClientProgressInsightDTO result = service.getSummary(5);

        assertThat(result.getNarrative()).isEqualTo("narrative");
    }

    @Test
    void getSummary_throwsWhenClientNotFound_onCacheMiss() {
        when(cacheManager.getCache(RedisConfig.CLIENT_PROGRESS_INSIGHT_CACHE)).thenReturn(cache);
        when(cache.get(5, ClientProgressInsightDTO.class)).thenReturn(null);
        when(clientRepository.existsById(5)).thenReturn(false);

        assertThatThrownBy(() -> service.getSummary(5))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("nije prona");
    }

    @Test
    void getSummary_propagatesAccessDeniedFromGuardAndNeverTouchesCacheOrClaude() {
        doThrow(new AccessDeniedException("denied")).when(trainerClientAccessGuard).assertCanAccessClient(5);

        assertThatThrownBy(() -> service.getSummary(5)).isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(cacheManager, claudeInsightService, clientProgressEntryRepository, clientPersonalRecordRepository);
    }

    @Test
    void getMySummary_resolvesClientFromJwtAndNeverCallsTheGuard() {
        Client client = Client.builder().id(42).build();
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("email", "client@gym.com")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(900))
                .build();
        Authentication authentication = new UsernamePasswordAuthenticationToken(jwt, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        when(clientRepository.findByUserEmail("client@gym.com")).thenReturn(Optional.of(client));
        when(cacheManager.getCache(RedisConfig.CLIENT_PROGRESS_INSIGHT_CACHE)).thenReturn(cache);
        when(cache.get(42, ClientProgressInsightDTO.class)).thenReturn(null);
        when(clientRepository.existsById(42)).thenReturn(true);
        when(clientProgressEntryRepository.findByClientIdOrderByEntryDateAsc(42)).thenReturn(List.of());
        when(clientPersonalRecordRepository.findByClientIdOrderByRecordDateDesc(42)).thenReturn(List.of());
        when(claudeInsightService.generate(anyString(), anyString())).thenReturn("narrative");

        ClientProgressInsightDTO result = service.getMySummary();

        assertThat(result.getClientId()).isEqualTo(42);
        verifyNoInteractions(trainerClientAccessGuard);
    }

    @Test
    void getMySummary_throwsWhenUnauthenticated() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(() -> service.getMySummary()).isInstanceOf(AccessDeniedException.class);
    }
}
