package com.example.demo.dto.insights;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Structured manager-dashboard payload for /manager/insights - superseded the original
 * single-{@code insightText}-string shape (see AGENTS.md "Upgrade: manager-insights dashboard
 * decisions"). The underlying analytics (room check-ins, distinct clients, average check-in
 * duration, paid appointments per session type, all over the same {@link #periodDays}-day
 * window) are unchanged; they are now exposed as real numbers ({@link #roomOccupancy},
 * {@link #sessionTypeBreakdown}, {@link #attendance}) for the frontend to chart with Recharts,
 * each carrying its own Claude-generated {@code rating}/{@code comment} rather than folding
 * everything into one prose paragraph. {@link #summary}/{@link #recommendations} remain as a
 * short overall wrap-up, now just one section of the dashboard instead of its entire content.
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ManagerInsightsDTO {
    private LocalDateTime generatedAt;
    private int periodDays;
    private String summary;
    private List<String> recommendations;
    private List<RoomOccupancyInsightDTO> roomOccupancy;
    private List<SessionTypeInsightDTO> sessionTypeBreakdown;
    private AttendanceInsightDTO attendance;
}
