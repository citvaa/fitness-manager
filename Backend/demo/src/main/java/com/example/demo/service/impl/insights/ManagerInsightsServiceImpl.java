package com.example.demo.service.impl.insights;

import com.example.demo.config.cache.RedisConfig;
import com.example.demo.dto.insights.ManagerInsightsDTO;
import com.example.demo.model.Payment;
import com.example.demo.model.gym.Room;
import com.example.demo.model.gym.RoomCheckIn;
import com.example.demo.repository.PaymentRepository;
import com.example.demo.repository.gym.RoomCheckInRepository;
import com.example.demo.repository.gym.RoomRepository;
import com.example.demo.service.ai.ClaudeInsightService;
import com.example.demo.service.insights.ManagerInsightsService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Aggregates room-occupancy history, attendance, and payment data into a short prompt and asks
 * Claude for a manager-facing summary/recommendation. See AGENTS.md ("Upgrade: service layer
 * decisions") for the model choice, caching strategy, and the revenue-proxy caveat (the schema
 * has no per-session price, only paid-appointment counts).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ManagerInsightsServiceImpl implements ManagerInsightsService {

    private static final int PERIOD_DAYS = 30;

    private static final String SYSTEM_PROMPT = """
            You are an analytics assistant for a gym manager. You are given aggregated,
            already-computed statistics about room occupancy history, attendance, and payments
            over the last %d days. Write a short (4-6 sentence) plain-text summary highlighting
            the most notable pattern(s) and one concrete, actionable recommendation. Do not
            invent numbers beyond what is given. No markdown, no headings, plain prose only.
            Respond in Serbian (srpski jezik), written in the Latin alphabet (latinica) - the
            rest of the application's UI is Serbian Latin script, so the summary must match it
            exactly; do not use Cyrillic (ćirilica).
            """.formatted(PERIOD_DAYS);

    private final RoomRepository roomRepository;
    private final RoomCheckInRepository roomCheckInRepository;
    private final PaymentRepository paymentRepository;
    private final ClaudeInsightService claudeInsightService;
    private final CacheManager cacheManager;

    @Override
    @Cacheable(value = RedisConfig.MANAGER_INSIGHTS_CACHE, key = "'current'")
    public ManagerInsightsDTO getInsights() {
        return generateInsights();
    }

    @Override
    public ManagerInsightsDTO refreshInsights() {
        ManagerInsightsDTO fresh = generateInsights();
        Cache cache = cacheManager.getCache(RedisConfig.MANAGER_INSIGHTS_CACHE);
        if (cache != null) {
            cache.put("current", fresh);
        }
        return fresh;
    }

    private ManagerInsightsDTO generateInsights() {
        String data = buildDataSummary();
        String insightText = claudeInsightService.generate(SYSTEM_PROMPT, data);

        ManagerInsightsDTO dto = new ManagerInsightsDTO();
        dto.setInsightText(insightText);
        dto.setGeneratedAt(LocalDateTime.now());
        dto.setPeriodDays(PERIOD_DAYS);
        return dto;
    }

    private String buildDataSummary() {
        LocalDateTime sinceDateTime = LocalDateTime.now().minusDays(PERIOD_DAYS);
        LocalDate sinceDate = LocalDate.now().minusDays(PERIOD_DAYS);

        List<RoomCheckIn> checkIns = roomCheckInRepository.findByCheckedInAtAfter(sinceDateTime);
        List<Room> rooms = roomRepository.findAll();
        List<Payment> payments = paymentRepository.findByPaymentDateAfter(sinceDate);

        Map<String, Long> checkInsByRoom = checkIns.stream()
                .collect(Collectors.groupingBy(c -> c.getRoom().getName(), Collectors.counting()));

        long distinctClients = checkIns.stream()
                .map(c -> c.getClient().getId())
                .distinct()
                .count();

        double avgCheckInDurationMinutes = checkIns.stream()
                .filter(c -> c.getCheckedOutAt() != null)
                .mapToLong(c -> Duration.between(c.getCheckedInAt(), c.getCheckedOutAt()).toMinutes())
                .average()
                .orElse(0);

        Map<String, Integer> paidAppointmentsBySessionType = payments.stream()
                .collect(Collectors.groupingBy(
                        p -> p.getSession().getType().name(),
                        Collectors.summingInt(Payment::getPaidAppointments)));

        StringBuilder sb = new StringBuilder();
        sb.append("Period: last ").append(PERIOD_DAYS).append(" days\n");
        sb.append("Total rooms: ").append(rooms.size()).append("\n");
        sb.append("Total room check-ins: ").append(checkIns.size()).append("\n");
        sb.append("Distinct clients checked in: ").append(distinctClients).append("\n");
        sb.append("Average check-in duration (minutes, closed check-ins only): ")
                .append(String.format("%.1f", avgCheckInDurationMinutes)).append("\n");
        sb.append("Check-ins per room:\n");
        checkInsByRoom.forEach((room, count) -> sb.append("  - ").append(room).append(": ").append(count).append("\n"));
        sb.append("Payments recorded: ").append(payments.size()).append("\n");
        sb.append("Paid appointments purchased per session type (proxy for revenue - the schema has no per-session price):\n");
        paidAppointmentsBySessionType.forEach((type, count) -> sb.append("  - ").append(type).append(": ").append(count).append("\n"));

        return sb.toString();
    }
}
