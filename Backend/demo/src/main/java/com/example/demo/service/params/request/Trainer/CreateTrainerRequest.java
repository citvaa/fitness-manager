package com.example.demo.service.params.request.Trainer;

import com.example.demo.enums.EmploymentStatus;
import com.example.demo.service.params.request.User.CreateUserRequest;
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
    private CreateUserRequest createUserRequest;
    private LocalDate employmentDate;
    private Integer birthYear;
    private EmploymentStatus status;
}
