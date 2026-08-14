package com.example.demo.service.impl.insights;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Shape of the strict-JSON response {@code ManagerInsightsServiceImpl} asks Claude for - mirrors
 * the structured {@code ManagerInsightsDTO} fields it gets merged into, one-to-one, instead of
 * the old free-text-plus-regex-parsing approach. {@code @JsonIgnoreProperties(ignoreUnknown =
 * true)} and the null-tolerant merge in {@code ManagerInsightsServiceImpl.mergeRating} mean an
 * imperfect/partial response degrades gracefully (missing items fall back to a generic
 * AVERAGE rating) rather than failing the whole request.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
class ClaudeManagerInsightResponse {
    public String summary;
    public List<String> recommendations;
    public List<RatedItem> roomRatings;
    public List<RatedItem> sessionTypeRatings;
    public RatedItem attendanceRating;

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class RatedItem {
        /** Present on room-rating entries; matched against the room name (case-insensitive). */
        public String roomName;
        /** Present on session-type-rating entries; matched against the Session.type enum name. */
        public String sessionType;
        /** One of InsightRating's names (EXCELLENT/GOOD/AVERAGE/POOR); anything else falls back to AVERAGE. */
        public String rating;
        public String comment;
    }
}
