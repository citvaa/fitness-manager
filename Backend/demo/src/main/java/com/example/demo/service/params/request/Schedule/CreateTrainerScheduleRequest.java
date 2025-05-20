package com.example.demo.service.params.request.Schedule;

import com.example.demo.enums.WorkStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class CreateTrainerScheduleRequest {
    private CreateScheduleRequest createScheduleRequest;
    private Integer trainerId;
    private WorkStatus status;
}
