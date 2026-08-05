package com.example.demo.service.params.request.progress;

import com.example.demo.enums.RecordUnit;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class UpsertPersonalRecordRequest {
    private String exerciseName;
    private BigDecimal value;
    private RecordUnit unit;
    private LocalDate recordDate;
}
