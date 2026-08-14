package com.example.demo.dto.insights;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Overall attendance numbers for the insights period, plus Claude's verdict on them. */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class AttendanceInsightDTO {
    private long distinctClients;
    private long totalCheckIns;
    private double avgCheckInDurationMinutes;
    private InsightRating rating;
    private String comment;
}
