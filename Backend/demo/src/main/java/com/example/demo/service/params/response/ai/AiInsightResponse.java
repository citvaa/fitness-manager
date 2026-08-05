package com.example.demo.service.params.response.ai;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AiInsightResponse {
    private String text;
    private String model;
    private LocalDateTime generatedAt;
}
