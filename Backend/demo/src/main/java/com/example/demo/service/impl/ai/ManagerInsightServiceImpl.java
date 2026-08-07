package com.example.demo.service.impl.ai;

import com.example.demo.model.Appointment;
import com.example.demo.model.Payment;
import com.example.demo.model.gym.RoomCheckIn;
import com.example.demo.repository.AppointmentRepository;
import com.example.demo.repository.PaymentRepository;
import com.example.demo.repository.gym.RoomCheckInRepository;
import com.example.demo.service.ai.ClaudeService;
import com.example.demo.service.ai.ManagerInsightService;
import com.example.demo.service.gym.OccupancyService;
import com.example.demo.service.params.response.ai.AiInsightResponse;
import com.example.demo.service.params.response.gym.OccupancySnapshotResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ManagerInsightServiceImpl implements ManagerInsightService {
    public static final String CACHE = "managerInsights";
    private final ClaudeService claudeService;
    private final OccupancyService occupancyService;
    private final RoomCheckInRepository checkInRepository;
    private final AppointmentRepository appointmentRepository;
    private final PaymentRepository paymentRepository;
    private final CacheManager cacheManager;

    public AiInsightResponse getInsights(boolean forceRegeneration) {
        Cache cache = cacheManager.getCache(CACHE);
        if (cache != null && !forceRegeneration) {
            AiInsightResponse cached = cache.get("current", AiInsightResponse.class);
            if (cached != null) return cached;
        }
        OccupancySnapshotResponse live = occupancyService.currentOccupancy();
        LocalDate today = live.generatedAt().toLocalDate();
        LocalDate from = today.minusDays(29);
        List<RoomCheckIn> checkIns = checkInRepository.findByCheckedInAtBetween(from.atStartOfDay(), today.plusDays(1).atStartOfDay());
        List<Appointment> appointments = appointmentRepository.findAll().stream().filter(a -> !a.getDate().isBefore(from) && !a.getDate().isAfter(today)).toList();
        List<Payment> payments = paymentRepository.findByPaymentDateBetween(from, today);
        long appointmentVisits = appointments.stream().mapToLong(a -> a.getClientAppointments() == null ? 0 : a.getClientAppointments().size()).sum();
        int soldAppointments = payments.stream().mapToInt(Payment::getPaidAppointments).sum();
        String data = "Period: " + from + " to " + today + "\nManual room visits: " + checkIns.size()
                + "\nScheduled client visits: " + appointmentVisits + "\nPayments recorded: " + payments.size()
                + "\nPaid appointment units sold (revenue proxy; prices are not stored): " + soldAppointments
                + "\nLive room occupancy: " + live.rooms();
        String text = claudeService.generate(
                "You are a gym operations analyst. Reply in Serbian with 3-5 concise, actionable observations. "
                        + "Use plain text only: do not use Markdown, headings, hash characters, asterisks, bullets, or other formatting syntax. "
                        + "Separate observations with blank lines. Do not invent monetary revenue because the input has no prices.", data);
        AiInsightResponse response = new AiInsightResponse(text, claudeService.model(), LocalDateTime.now());
        if (cache != null) cache.put("current", response);
        return response;
    }
}
