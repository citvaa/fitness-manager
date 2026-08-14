package com.example.demo.dto.insights;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Paid-appointment count for one {@code Session.type} (INDIVIDUAL/GROUP) over the insights
 * period, plus Claude's verdict on the individual/group mix. "Paid appointments" is the same
 * revenue-proxy metric the rest of this feature uses (see ManagerInsightsServiceImpl) - the
 * schema has no per-session price.
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class SessionTypeInsightDTO {
    private String sessionType;
    private int paidAppointments;
    /** This session type's share of total paid appointments across all types, 0-100. */
    private double sharePercent;
    private InsightRating rating;
    private String comment;
}
