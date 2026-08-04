package com.example.demo.dto.progress;

import com.example.demo.dto.summary.ClientSummaryDTO;
import com.example.demo.enums.RecordUnit;
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
public class ClientPersonalRecordDTO {
    private Integer id;
    private ClientSummaryDTO client;
    private String exerciseName;
    private BigDecimal value;
    private RecordUnit unit;
    private LocalDate recordDate;
    private String notes;
}
