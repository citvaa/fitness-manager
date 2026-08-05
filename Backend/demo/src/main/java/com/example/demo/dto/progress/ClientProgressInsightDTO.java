package com.example.demo.dto.progress;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ClientProgressInsightDTO {
    private Integer clientId;
    private String narrative;
    private LocalDateTime generatedAt;
}
