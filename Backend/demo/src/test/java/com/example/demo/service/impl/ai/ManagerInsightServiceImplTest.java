package com.example.demo.service.impl.ai;

import com.example.demo.repository.AppointmentRepository;
import com.example.demo.repository.PaymentRepository;
import com.example.demo.repository.gym.RoomCheckInRepository;
import com.example.demo.service.ai.ClaudeService;
import com.example.demo.service.gym.OccupancyService;
import com.example.demo.service.params.response.ai.AiInsightResponse;
import com.example.demo.service.params.response.gym.OccupancySnapshotResponse;
import org.junit.jupiter.api.Test;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import java.time.LocalDateTime;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ManagerInsightServiceImplTest {
    @Test void cachesInsightAndForceRegenerationBypassesCache() {
        ClaudeService claude=mock(ClaudeService.class); OccupancyService occupancy=mock(OccupancyService.class);
        var checkIns=mock(RoomCheckInRepository.class); var appointments=mock(AppointmentRepository.class); var payments=mock(PaymentRepository.class);
        var caches=new ConcurrentMapCacheManager(ManagerInsightServiceImpl.CACHE);
        when(occupancy.currentOccupancy()).thenReturn(new OccupancySnapshotResponse(LocalDateTime.of(2026,8,7,12,0), List.of()));
        when(checkIns.findByCheckedInAtBetween(any(),any())).thenReturn(List.of()); when(appointments.findAll()).thenReturn(List.of());
        when(payments.findByPaymentDateBetween(any(),any())).thenReturn(List.of()); when(claude.generate(anyString(),anyString())).thenReturn("prvi","drugi");
        when(claude.model()).thenReturn("fake-model");
        var service=new ManagerInsightServiceImpl(claude,occupancy,checkIns,appointments,payments,caches);
        AiInsightResponse first=service.getInsights(false); AiInsightResponse cached=service.getInsights(false);
        assertSame(first,cached); verify(claude,times(1)).generate(anyString(),anyString());
        AiInsightResponse refreshed=service.getInsights(true);
        assertEquals("drugi",refreshed.getText()); verify(claude,times(2)).generate(anyString(),anyString());
    }
}
