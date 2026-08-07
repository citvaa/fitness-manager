package com.example.demo.service.impl.insights;

import com.example.demo.config.cache.RedisConfig;
import com.example.demo.dto.insights.ManagerInsightsDTO;
import com.example.demo.enums.SessionType;
import com.example.demo.model.Payment;
import com.example.demo.model.Session;
import com.example.demo.model.gym.Room;
import com.example.demo.model.gym.RoomCheckIn;
import com.example.demo.model.user.Client;
import com.example.demo.repository.PaymentRepository;
import com.example.demo.repository.gym.RoomCheckInRepository;
import com.example.demo.repository.gym.RoomRepository;
import com.example.demo.service.ai.ClaudeInsightService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link ManagerInsightsServiceImpl} - the Claude prompt/data-aggregation logic
 * (with the Claude client fully mocked, no network call) and the two-method cache interplay
 * (getInsights() relies on @Cacheable via the Spring proxy - not exercised here since this is a
 * plain-Mockito unit test, so we instead verify the underlying generateInsights() logic and the
 * refreshInsights() manual evict+repopulate path against a mocked CacheManager/Cache, per
 * AGENTS.md "Upgrade: service layer decisions").
 */
@ExtendWith(MockitoExtension.class)
class ManagerInsightsServiceImplTest {

    @Mock
    private RoomRepository roomRepository;
    @Mock
    private RoomCheckInRepository roomCheckInRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private ClaudeInsightService claudeInsightService;
    @Mock
    private CacheManager cacheManager;
    @Mock
    private Cache cache;

    private ManagerInsightsServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ManagerInsightsServiceImpl(roomRepository, roomCheckInRepository, paymentRepository,
                claudeInsightService, cacheManager);
    }

    @Test
    void getInsights_generatesTextViaClaudeAndSetsPeriodDays() {
        when(roomRepository.findAll()).thenReturn(List.of());
        when(roomCheckInRepository.findByCheckedInAtAfter(any())).thenReturn(List.of());
        when(paymentRepository.findByPaymentDateAfter(any())).thenReturn(List.of());
        when(claudeInsightService.generate(anyString(), anyString())).thenReturn("Generated summary.");

        ManagerInsightsDTO dto = service.getInsights();

        assertThat(dto.getInsightText()).isEqualTo("Generated summary.");
        assertThat(dto.getPeriodDays()).isEqualTo(30);
        assertThat(dto.getGeneratedAt()).isNotNull();
    }

    @Test
    void getInsights_neverInventsNumbers_promptIncludesAggregatedRealData() {
        Room studio = Room.builder().id(1).name("Studio A").capacity(10).build();
        Client client1 = Client.builder().id(1).build();
        Client client2 = Client.builder().id(2).build();

        RoomCheckIn closedCheckIn = RoomCheckIn.builder()
                .id(1).room(studio).client(client1)
                .checkedInAt(LocalDateTime.now().minusHours(2))
                .checkedOutAt(LocalDateTime.now().minusHours(1))
                .build();
        RoomCheckIn openCheckIn = RoomCheckIn.builder()
                .id(2).room(studio).client(client2)
                .checkedInAt(LocalDateTime.now())
                .build();

        Session groupSession = new Session();
        groupSession.setId(1);
        groupSession.setType(SessionType.GROUP);

        Payment payment = Payment.builder().id(1).client(client1).session(groupSession)
                .paidAppointments(4).paymentDate(LocalDate.now()).build();

        when(roomRepository.findAll()).thenReturn(List.of(studio));
        when(roomCheckInRepository.findByCheckedInAtAfter(any())).thenReturn(List.of(closedCheckIn, openCheckIn));
        when(paymentRepository.findByPaymentDateAfter(any())).thenReturn(List.of(payment));
        when(claudeInsightService.generate(anyString(), anyString())).thenReturn("summary");

        service.getInsights();

        ArgumentCaptor<String> systemPromptCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> dataCaptor = ArgumentCaptor.forClass(String.class);
        verify(claudeInsightService).generate(systemPromptCaptor.capture(), dataCaptor.capture());

        // Revenue-proxy caveat must be explicit in the prompt - see AGENTS.md.
        assertThat(systemPromptCaptor.getValue()).contains("Respond in Serbian");
        assertThat(dataCaptor.getValue()).contains("Total rooms: 1");
        assertThat(dataCaptor.getValue()).contains("Total room check-ins: 2");
        assertThat(dataCaptor.getValue()).contains("Distinct clients checked in: 2");
        assertThat(dataCaptor.getValue()).contains("Studio A: 2");
        assertThat(dataCaptor.getValue()).contains("proxy for revenue");
        assertThat(dataCaptor.getValue()).contains("GROUP: 4");
    }

    @Test
    void refreshInsights_regeneratesAndRepopulatesCache() {
        when(roomRepository.findAll()).thenReturn(List.of());
        when(roomCheckInRepository.findByCheckedInAtAfter(any())).thenReturn(List.of());
        when(paymentRepository.findByPaymentDateAfter(any())).thenReturn(List.of());
        when(claudeInsightService.generate(anyString(), anyString())).thenReturn("Fresh text");
        when(cacheManager.getCache(RedisConfig.MANAGER_INSIGHTS_CACHE)).thenReturn(cache);

        ManagerInsightsDTO result = service.refreshInsights();

        assertThat(result.getInsightText()).isEqualTo("Fresh text");
        verify(cache).put(eq("current"), eq(result));
    }

    @Test
    void refreshInsights_doesNotThrowWhenCacheRegionMissing() {
        when(roomRepository.findAll()).thenReturn(List.of());
        when(roomCheckInRepository.findByCheckedInAtAfter(any())).thenReturn(List.of());
        when(paymentRepository.findByPaymentDateAfter(any())).thenReturn(List.of());
        when(claudeInsightService.generate(anyString(), anyString())).thenReturn("text");
        when(cacheManager.getCache(RedisConfig.MANAGER_INSIGHTS_CACHE)).thenReturn(null);

        ManagerInsightsDTO result = service.refreshInsights();

        assertThat(result.getInsightText()).isEqualTo("text");
        // No cache to put into - just verifying no NPE was thrown getting here.
    }

    @Test
    void refreshInsights_alwaysCallsClaudeAgain_neverReadsFromCache() {
        // Distinguishes refreshInsights() from getInsights(): it must always regenerate, not
        // check the cache first - see AGENTS.md's "getInsights()/refreshInsights() never call
        // each other internally" note.
        when(roomRepository.findAll()).thenReturn(List.of());
        when(roomCheckInRepository.findByCheckedInAtAfter(any())).thenReturn(List.of());
        when(paymentRepository.findByPaymentDateAfter(any())).thenReturn(List.of());
        when(claudeInsightService.generate(anyString(), anyString())).thenReturn("a").thenReturn("b");
        when(cacheManager.getCache(RedisConfig.MANAGER_INSIGHTS_CACHE)).thenReturn(cache);

        service.refreshInsights();
        service.refreshInsights();

        verify(claudeInsightService, times(2)).generate(anyString(), anyString());
        verify(cache, never()).get(any());
    }
}
