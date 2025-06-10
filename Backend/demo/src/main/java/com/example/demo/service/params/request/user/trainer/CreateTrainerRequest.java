package com.example.demo.service.params.request.user.trainer;

import com.example.demo.enums.EmploymentStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class CreateTrainerRequest {
    private String email;
    private LocalDate employmentDate;
    private Integer birthYear;
    private EmploymentStatus status;
}
