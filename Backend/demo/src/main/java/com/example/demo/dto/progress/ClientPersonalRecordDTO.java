package com.example.demo.dto.progress;

import com.example.demo.dto.summary.ClientSummaryDTO;
import com.example.demo.enums.RecordUnit;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ClientPersonalRecordDTO {
    private Integer id;
    private ClientSummaryDTO client;
    private String exerciseName;
    private BigDecimal value;
    private RecordUnit unit;
    private LocalDate recordDate;
}
