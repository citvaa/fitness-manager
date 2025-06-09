package com.example.demo.dto.notification;

import com.example.demo.dto.AppointmentDTO;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class TrainerAppointmentNotificationDTO {
    private AppointmentDTO appointment;
}
