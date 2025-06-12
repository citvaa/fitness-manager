package com.example.demo.dto.user;

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
public class TrainerDTO {
    private Integer id;
    private UserDTO user;
    private LocalDate employmentDate;
    private Integer birthYear;
    private EmploymentStatus status;
}
