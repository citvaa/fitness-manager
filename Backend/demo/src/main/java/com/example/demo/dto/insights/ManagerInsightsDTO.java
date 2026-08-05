package com.example.demo.dto.insights;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ManagerInsightsDTO {
    private String insightText;
    private LocalDateTime generatedAt;
    private int periodDays;
}
