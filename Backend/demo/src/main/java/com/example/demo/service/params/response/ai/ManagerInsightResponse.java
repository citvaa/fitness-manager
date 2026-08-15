package com.example.demo.service.params.response.ai;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ManagerInsightResponse {
    private String summary;
    private List<String> recommendations;
    private List<ManagerMetricInsight> metrics;
    private String model;
    private LocalDateTime generatedAt;
}
