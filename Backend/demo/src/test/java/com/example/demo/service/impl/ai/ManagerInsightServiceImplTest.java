package com.example.demo.service.impl.ai;

import com.example.demo.enums.SessionType;
import com.example.demo.model.Appointment;
import com.example.demo.model.Session;
import com.example.demo.repository.AppointmentRepository;
import com.example.demo.repository.PaymentRepository;
import com.example.demo.repository.gym.RoomCheckInRepository;
import com.example.demo.service.ai.ClaudeService;
import com.example.demo.service.gym.OccupancyService;
import com.example.demo.service.params.response.ai.InsightRating;
import com.example.demo.service.params.response.gym.OccupancySnapshotResponse;
import com.example.demo.service.params.response.gym.RoomOccupancyResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ManagerInsightServiceImplTest {
    @Test void calculatesMetricsCachesResponseAndFallsBackForOmittedAiElements() {
        ClaudeService claude=mock(ClaudeService.class); OccupancyService occupancy=mock(OccupancyService.class);
        var checkIns=mock(RoomCheckInRepository.class); var appointments=mock(AppointmentRepository.class); var payments=mock(PaymentRepository.class);
        var caches=new ConcurrentMapCacheManager(ManagerInsightServiceImpl.CACHE);
        LocalDateTime generated = LocalDateTime.of(2026,8,7,12,0);
        when(occupancy.currentOccupancy()).thenReturn(new OccupancySnapshotResponse(generated,
                List.of(new RoomOccupancyResponse(4,"Studio",10,1,3,4))));
        when(checkIns.findByCheckedInAtBetween(any(),any())).thenReturn(List.of());
        Appointment appointment = Appointment.builder().date(LocalDate.of(2026,8,7))
                .session(new Session(1, SessionType.INDIVIDUAL, 1)).clientAppointments(new HashSet<>()).build();
        when(appointments.findAll()).thenReturn(List.of(appointment));
        when(payments.findByPaymentDateBetween(any(),any())).thenReturn(List.of());
        when(claude.generate(anyString(),anyString())).thenReturn("{\"summary\":\"Rezime\",\"recommendations\":[\"Prva\",\"Druga\"],\"metrics\":{\"room-4\":{\"rating\":\"GOOD\",\"comment\":\"Dobro\"}}}");
        when(claude.model()).thenReturn("fake-model");
        var service=new ManagerInsightServiceImpl(claude,occupancy,checkIns,appointments,payments,caches,new ObjectMapper().findAndRegisterModules());

        var first=service.getInsights(false); var cached=service.getInsights(false);

        assertSame(first,cached);
        assertEquals("Rezime", first.getSummary());
        assertEquals(5, first.getMetrics().size());
        assertEquals(40.0, first.getMetrics().getFirst().value());
        assertEquals(InsightRating.GOOD, first.getMetrics().getFirst().rating());
        assertTrue(first.getMetrics().stream().skip(1).allMatch(metric -> metric.rating() == InsightRating.AVERAGE));
        verify(claude,times(1)).generate(anyString(),anyString());
    }
}
