package com.example.demo.service.impl.insights;

import com.example.demo.config.cache.RedisConfig;
import com.example.demo.dto.insights.AttendanceInsightDTO;
import com.example.demo.dto.insights.InsightRating;
import com.example.demo.dto.insights.ManagerInsightsDTO;
import com.example.demo.dto.insights.RoomOccupancyInsightDTO;
import com.example.demo.dto.insights.SessionTypeInsightDTO;
import com.example.demo.model.Payment;
import com.example.demo.model.gym.Room;
import com.example.demo.model.gym.RoomCheckIn;
import com.example.demo.repository.PaymentRepository;
import com.example.demo.repository.gym.RoomCheckInRepository;
import com.example.demo.repository.gym.RoomRepository;
import com.example.demo.service.ai.ClaudeInsightService;
import com.example.demo.service.insights.ManagerInsightsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Aggregates room-occupancy history, attendance, and payment data into structured numbers (for
 * the manager-insights dashboard's Recharts visuals) and asks Claude for a short overall
 * summary/recommendations plus a per-item verdict on each number. See AGENTS.md ("Upgrade:
 * manager-insights dashboard decisions") for the DTO shape and the JSON-response contract with
 * Claude, and ("Upgrade: service layer decisions") for the model choice, caching strategy, and
 * the revenue-proxy caveat (the schema has no per-session price, only paid-appointment counts).
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ManagerInsightsServiceImpl implements ManagerInsightsService {

    private static final int PERIOD_DAYS = 30;

    // Asks for strict JSON (not the old free-text-plus-regex-on-the-frontend approach) so every
    // field maps directly onto ManagerInsightsDTO - see ClaudeManagerInsightResponse. Ratings are
    // constrained to InsightRating's exact names so the frontend can render a consistent colored
    // badge per item instead of parsing free text.
    private static final String SYSTEM_PROMPT = """
            You are an analytics assistant for a gym manager. You are given aggregated,
            already-computed statistics about room occupancy, attendance, and payments over the
            last %d days, structured as a room list, a session-type list, and overall attendance
            numbers.

            Respond with ONLY a single valid JSON object (no markdown code fences, no commentary
            before or after it) with exactly this shape:
            {
              "summary": "2-3 sentence overall summary of the most notable pattern(s)",
              "recommendations": ["2-4 short, concrete, actionable recommendations"],
              "roomRatings": [
                {"roomName": "<exact room name from the input>", "rating": "EXCELLENT|GOOD|AVERAGE|POOR", "comment": "one short sentence on how good this room's occupancy is"}
              ],
              "sessionTypeRatings": [
                {"sessionType": "<exact session type from the input>", "rating": "EXCELLENT|GOOD|AVERAGE|POOR", "comment": "one short sentence on this session type's share"}
              ],
              "attendanceRating": {"rating": "EXCELLENT|GOOD|AVERAGE|POOR", "comment": "one short sentence on overall attendance"}
            }

            Include one entry in "roomRatings" for every room given, and one entry in
            "sessionTypeRatings" for every session type given - matching "roomName"/"sessionType"
            exactly (same spelling/case) to what was given in the input. "rating" must be exactly
            one of EXCELLENT, GOOD, AVERAGE, POOR (uppercase, no other values). Do not invent
            numbers beyond what is given. Write all "summary"/"comment"/"recommendations" text in
            Serbian (srpski jezik), Latin alphabet (latinica) - the rest of the application's UI
            is Serbian Latin script, so this must match it exactly; do not use Cyrillic
            (ćirilica). Keep every comment to one short sentence.
            """.formatted(PERIOD_DAYS);

    private final RoomRepository roomRepository;
    private final RoomCheckInRepository roomCheckInRepository;
    private final PaymentRepository paymentRepository;
    private final ClaudeInsightService claudeInsightService;
    private final CacheManager cacheManager;
    private final ObjectMapper objectMapper;

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
        Metrics metrics = computeMetrics();
        String userPrompt = buildDataSummary(metrics);
        ClaudeManagerInsightResponse aiResponse = requestAiResponse(userPrompt);

        Map<String, ClaudeManagerInsightResponse.RatedItem> roomRatingsByName = (aiResponse.roomRatings == null
                ? List.<ClaudeManagerInsightResponse.RatedItem>of() : aiResponse.roomRatings).stream()
                .filter(r -> r.roomName != null)
                .collect(Collectors.toMap(r -> r.roomName.trim().toLowerCase(Locale.ROOT), r -> r, (a, b) -> a));

        Map<String, ClaudeManagerInsightResponse.RatedItem> sessionRatingsByType = (aiResponse.sessionTypeRatings == null
                ? List.<ClaudeManagerInsightResponse.RatedItem>of() : aiResponse.sessionTypeRatings).stream()
                .filter(r -> r.sessionType != null)
                .collect(Collectors.toMap(r -> r.sessionType.trim().toLowerCase(Locale.ROOT), r -> r, (a, b) -> a));

        List<RoomOccupancyInsightDTO> roomOccupancy = metrics.checkInsByRoom.entrySet().stream()
                .map(e -> {
                    ClaudeManagerInsightResponse.RatedItem rated = roomRatingsByName.get(e.getKey().trim().toLowerCase(Locale.ROOT));
                    double share = metrics.totalCheckIns == 0 ? 0 : 100.0 * e.getValue() / metrics.totalCheckIns;
                    return new RoomOccupancyInsightDTO(e.getKey(), e.getValue(), round1(share),
                            parseRating(rated == null ? null : rated.rating),
                            rated != null && rated.comment != null ? rated.comment : "Nema dovoljno podataka za ocenu.");
                })
                .sorted(Comparator.comparingLong(RoomOccupancyInsightDTO::getCheckIns).reversed())
                .collect(Collectors.toList());

        List<SessionTypeInsightDTO> sessionTypeBreakdown = metrics.paidAppointmentsBySessionType.entrySet().stream()
                .map(e -> {
                    ClaudeManagerInsightResponse.RatedItem rated = sessionRatingsByType.get(e.getKey().trim().toLowerCase(Locale.ROOT));
                    double share = metrics.totalPaidAppointments == 0 ? 0 : 100.0 * e.getValue() / metrics.totalPaidAppointments;
                    return new SessionTypeInsightDTO(e.getKey(), e.getValue(), round1(share),
                            parseRating(rated == null ? null : rated.rating),
                            rated != null && rated.comment != null ? rated.comment : "Nema dovoljno podataka za ocenu.");
                })
                .collect(Collectors.toList());

        AttendanceInsightDTO attendance = new AttendanceInsightDTO(
                metrics.distinctClients,
                metrics.totalCheckIns,
                round1(metrics.avgCheckInDurationMinutes),
                parseRating(aiResponse.attendanceRating == null ? null : aiResponse.attendanceRating.rating),
                aiResponse.attendanceRating != null && aiResponse.attendanceRating.comment != null
                        ? aiResponse.attendanceRating.comment : "Nema dovoljno podataka za ocenu.");

        ManagerInsightsDTO dto = new ManagerInsightsDTO();
        dto.setGeneratedAt(LocalDateTime.now());
        dto.setPeriodDays(PERIOD_DAYS);
        dto.setSummary(aiResponse.summary != null ? aiResponse.summary : "");
        dto.setRecommendations(aiResponse.recommendations != null ? aiResponse.recommendations : List.of());
        dto.setRoomOccupancy(roomOccupancy);
        dto.setSessionTypeBreakdown(sessionTypeBreakdown);
        dto.setAttendance(attendance);
        return dto;
    }

    private ClaudeManagerInsightResponse requestAiResponse(String userPrompt) {
        String rawText = claudeInsightService.generate(SYSTEM_PROMPT, userPrompt);
        String json = stripCodeFence(rawText);
        try {
            return objectMapper.readValue(json, ClaudeManagerInsightResponse.class);
        } catch (Exception e) {
            log.error("❌ Failed to parse Claude manager-insights JSON response: {}", e.getMessage());
            throw new IllegalStateException("AI odgovor nije u očekivanom formatu - pokušaj ponovo.", e);
        }
    }

    /** Claude occasionally wraps JSON in ```json ... ``` despite instructions not to - strip it defensively. */
    private String stripCodeFence(String text) {
        String trimmed = text.trim();
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceFirst("^```[a-zA-Z]*\\s*", "");
            if (trimmed.endsWith("```")) {
                trimmed = trimmed.substring(0, trimmed.length() - 3);
            }
        }
        return trimmed.trim();
    }

    private InsightRating parseRating(String raw) {
        if (raw == null) return InsightRating.AVERAGE;
        try {
            return InsightRating.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return InsightRating.AVERAGE;
        }
    }

    private double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private record Metrics(
            Map<String, Long> checkInsByRoom,
            long totalCheckIns,
            long distinctClients,
            double avgCheckInDurationMinutes,
            Map<String, Integer> paidAppointmentsBySessionType,
            int totalPaidAppointments) {
    }

    private Metrics computeMetrics() {
        LocalDateTime sinceDateTime = LocalDateTime.now().minusDays(PERIOD_DAYS);
        LocalDate sinceDate = LocalDate.now().minusDays(PERIOD_DAYS);

        List<RoomCheckIn> checkIns = roomCheckInRepository.findByCheckedInAtAfter(sinceDateTime);
        List<Room> rooms = roomRepository.findAll();
        List<Payment> payments = paymentRepository.findByPaymentDateAfter(sinceDate);

        // Every room is included, even with zero check-ins - the dashboard's per-room chart
        // should show the full floor plan's occupancy, not just rooms that happened to have any.
        Map<String, Long> checkInsByName = checkIns.stream()
                .collect(Collectors.groupingBy(c -> c.getRoom().getName(), Collectors.counting()));
        Map<String, Long> checkInsByRoom = new LinkedHashMap<>();
        rooms.forEach(r -> checkInsByRoom.put(r.getName(), checkInsByName.getOrDefault(r.getName(), 0L)));

        long distinctClients = checkIns.stream()
                .map(c -> c.getClient().getId())
                .distinct()
                .count();

        double avgCheckInDurationMinutes = checkIns.stream()
                .filter(c -> c.getCheckedOutAt() != null)
                .mapToLong(c -> Duration.between(c.getCheckedInAt(), c.getCheckedOutAt()).toMinutes())
                .average()
                .orElse(0);

        Map<String, Integer> paidAppointmentsBySessionType = new LinkedHashMap<>();
        payments.forEach(p -> paidAppointmentsBySessionType.merge(
                p.getSession().getType().name(), p.getPaidAppointments(), Integer::sum));
        int totalPaidAppointments = paidAppointmentsBySessionType.values().stream().mapToInt(Integer::intValue).sum();

        return new Metrics(checkInsByRoom, checkIns.size(), distinctClients, avgCheckInDurationMinutes,
                paidAppointmentsBySessionType, totalPaidAppointments);
    }

    private String buildDataSummary(Metrics metrics) {
        StringBuilder sb = new StringBuilder();
        sb.append("Period: last ").append(PERIOD_DAYS).append(" days\n");
        sb.append("Total room check-ins: ").append(metrics.totalCheckIns).append("\n");
        sb.append("Distinct clients checked in: ").append(metrics.distinctClients).append("\n");
        sb.append("Average check-in duration (minutes, closed check-ins only): ")
                .append(String.format(Locale.ROOT, "%.1f", metrics.avgCheckInDurationMinutes)).append("\n");
        sb.append("Check-ins per room:\n");
        metrics.checkInsByRoom.forEach((room, count) -> sb.append("  - ").append(room).append(": ").append(count).append("\n"));
        sb.append("Paid appointments purchased per session type (proxy for revenue - the schema has no per-session price):\n");
        metrics.paidAppointmentsBySessionType.forEach((type, count) -> sb.append("  - ").append(type).append(": ").append(count).append("\n"));
        return sb.toString();
    }
}
