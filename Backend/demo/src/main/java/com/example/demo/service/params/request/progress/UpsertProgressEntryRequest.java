package com.example.demo.service.params.request.progress;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class UpsertProgressEntryRequest {
    private LocalDate entryDate;
    private BigDecimal weightKg;
    private BigDecimal bodyFatPercent;
    private BigDecimal waistCm;
    private BigDecimal chestCm;
    private BigDecimal hipCm;
    private BigDecimal thighCm;
    private BigDecimal armCm;
    private String notes;
}
