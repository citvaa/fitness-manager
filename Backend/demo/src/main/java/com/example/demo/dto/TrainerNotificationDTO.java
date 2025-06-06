package com.example.demo.dto;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class TrainerNotificationDTO {
    private AppointmentDTO appointment;
}
