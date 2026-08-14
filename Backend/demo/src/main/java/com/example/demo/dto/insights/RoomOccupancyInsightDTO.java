package com.example.demo.dto.insights;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** One room's check-in count over the insights period, plus Claude's per-room verdict on it. */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class RoomOccupancyInsightDTO {
    private String roomName;
    private long checkIns;
    /** This room's share of total check-ins across all rooms, 0-100. */
    private double sharePercent;
    private InsightRating rating;
    private String comment;
}
