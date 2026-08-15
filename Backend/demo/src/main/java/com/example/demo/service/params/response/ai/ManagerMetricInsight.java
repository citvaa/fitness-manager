package com.example.demo.service.params.response.ai;

public record ManagerMetricInsight(String key, String label, double value, String unit,
                                   InsightRating rating, String comment) {}
