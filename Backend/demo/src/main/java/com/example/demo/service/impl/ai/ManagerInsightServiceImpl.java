package com.example.demo.service.impl.ai;

import com.example.demo.enums.SessionType;
import com.example.demo.model.Appointment;
import com.example.demo.model.Payment;
import com.example.demo.model.gym.RoomCheckIn;
import com.example.demo.repository.AppointmentRepository;
import com.example.demo.repository.PaymentRepository;
import com.example.demo.repository.gym.RoomCheckInRepository;
import com.example.demo.service.ai.ClaudeService;
import com.example.demo.service.ai.ManagerInsightService;
import com.example.demo.service.gym.OccupancyService;
import com.example.demo.service.params.response.ai.InsightRating;
import com.example.demo.service.params.response.ai.ManagerInsightResponse;
import com.example.demo.service.params.response.ai.ManagerMetricInsight;
import com.example.demo.service.params.response.gym.OccupancySnapshotResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ManagerInsightServiceImpl implements ManagerInsightService {
    public static final String CACHE = "managerInsights";
    private static final String FALLBACK_COMMENT = "Pratite ovaj pokazatelj i uporedite ga sa narednim periodom.";
    private final ClaudeService claudeService;
    private final OccupancyService occupancyService;
    private final RoomCheckInRepository checkInRepository;
    private final AppointmentRepository appointmentRepository;
    private final PaymentRepository paymentRepository;
    private final CacheManager cacheManager;
    private final ObjectMapper objectMapper;

    public ManagerInsightResponse getInsights(boolean forceRegeneration) {
        Cache cache = cacheManager.getCache(CACHE);
        if (cache != null && !forceRegeneration) {
            ManagerInsightResponse cached = cache.get("current", ManagerInsightResponse.class);
            if (cached != null) return cached;
        }
        OccupancySnapshotResponse live = occupancyService.currentOccupancy();
        LocalDate today = live.generatedAt().toLocalDate();
        LocalDate from = today.minusDays(29);
        List<RoomCheckIn> checkIns = checkInRepository.findByCheckedInAtBetween(from.atStartOfDay(), today.plusDays(1).atStartOfDay());
        List<Appointment> appointments = appointmentRepository.findAll().stream().filter(item -> !item.getDate().isBefore(from) && !item.getDate().isAfter(today)).toList();
        List<Payment> payments = paymentRepository.findByPaymentDateBetween(from, today);
        List<MetricValue> values = calculateMetrics(live, checkIns, appointments, payments);
        AiCopy copy = generateCopy(from, today, values);
        List<ManagerMetricInsight> metrics = values.stream().map(value -> {
            JsonNode aiMetric = copy.metrics().path(value.key());
            return new ManagerMetricInsight(value.key(), value.label(), value.value(), value.unit(),
                    rating(aiMetric.path("rating").asText()),
                    aiMetric.path("comment").asText(FALLBACK_COMMENT));
        }).toList();
        ManagerInsightResponse response = new ManagerInsightResponse(copy.summary(), copy.recommendations(), metrics,
                claudeService.model(), LocalDateTime.now());
        if (cache != null) cache.put("current", response);
        return response;
    }

    private List<MetricValue> calculateMetrics(OccupancySnapshotResponse live, List<RoomCheckIn> checkIns,
                                                List<Appointment> appointments, List<Payment> payments) {
        List<MetricValue> values = new ArrayList<>();
        live.rooms().forEach(room -> values.add(new MetricValue("room-" + room.roomId(),
                "Popunjenost · " + room.roomName(), percentage(room.totalOccupancy(), room.capacity()), "%")));
        long individual = appointments.stream().filter(item -> item.getSession().getType() == SessionType.INDIVIDUAL).count();
        long group = appointments.size() - individual;
        values.add(new MetricValue("individual-mix", "Udeo individualnih sesija", percentage(individual, appointments.size()), "%"));
        values.add(new MetricValue("group-mix", "Udeo grupnih sesija", percentage(group, appointments.size()), "%"));
        long scheduledVisits = appointments.stream().mapToLong(item -> item.getClientAppointments() == null ? 0 : item.getClientAppointments().size()).sum();
        values.add(new MetricValue("attendance", "Check-in prema rezervacijama", percentage(checkIns.size(), scheduledVisits), "%"));
        values.add(new MetricValue("sold-units", "Plaćeni termini", payments.stream().mapToInt(Payment::getPaidAppointments).sum(), "termina"));
        return values;
    }

    private AiCopy generateCopy(LocalDate from, LocalDate today, List<MetricValue> values) {
        try {
            String prompt = objectMapper.writeValueAsString(java.util.Map.of("periodFrom", from, "periodTo", today, "metrics", values));
            String raw = claudeService.generate("Return only valid JSON with summary, recommendations (2-4 Serbian strings), and metrics object keyed exactly as provided. Each metric must contain rating EXCELLENT/GOOD/AVERAGE/POOR and a concise Serbian comment. Never change or invent numeric values.", prompt);
            JsonNode root = objectMapper.readTree(raw.replaceFirst("^```(?:json)?\\s*", "").replaceFirst("\\s*```$", ""));
            List<String> recommendations = new ArrayList<>();
            root.path("recommendations").forEach(item -> recommendations.add(item.asText()));
            List<String> finalRecommendations = recommendations.isEmpty() ? fallbackRecommendations() : recommendations;
            return new AiCopy(root.path("summary").asText("Pregled operativnih pokazatelja za poslednjih 30 dana."), finalRecommendations, root.path("metrics"));
        } catch (Exception ignored) {
            return new AiCopy("Pregled operativnih pokazatelja za poslednjih 30 dana.", fallbackRecommendations(), objectMapper.createObjectNode());
        }
    }

    private List<String> fallbackRecommendations() {
        return List.of("Uporedite pokazatelje sa prethodnih 30 dana.", "Proverite termine i sale sa najvećim odstupanjem.");
    }

    private InsightRating rating(String value) {
        try { return InsightRating.valueOf(value); }
        catch (RuntimeException ignored) { return InsightRating.AVERAGE; }
    }

    private double percentage(long part, long total) { return total == 0 ? 0 : Math.round(part * 1000.0 / total) / 10.0; }
    private record MetricValue(String key, String label, double value, String unit) {}
    private record AiCopy(String summary, List<String> recommendations, JsonNode metrics) {}
}
