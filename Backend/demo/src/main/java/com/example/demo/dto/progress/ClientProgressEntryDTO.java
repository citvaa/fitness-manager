package com.example.demo.dto.progress;

import com.example.demo.dto.summary.ClientSummaryDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ClientProgressEntryDTO {
    private Integer id;
    private ClientSummaryDTO client;
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
