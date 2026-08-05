package com.example.demo.service.params.request.progress;

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
public class CreatePersonalRecordRequest {
    private Integer clientId;
    private String exerciseName;
    private BigDecimal value;
    private RecordUnit unit;
    private LocalDate recordDate;
    private String notes;
}
