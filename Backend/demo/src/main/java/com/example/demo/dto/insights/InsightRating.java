package com.example.demo.dto.insights;

/**
 * A short, per-item AI verdict ("how good is this number for the gym") attached to each
 * structured insight (room occupancy, session-type breakdown, attendance) - see
 * ManagerInsightsDTO. Deliberately a small fixed set (not a free-text label) so the frontend can
 * render a consistent colored badge per item instead of parsing free text.
 */
public enum InsightRating {
    EXCELLENT,
    GOOD,
    AVERAGE,
    POOR,
}
