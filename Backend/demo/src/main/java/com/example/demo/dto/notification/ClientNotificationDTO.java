package com.example.demo.dto.notification;

import com.example.demo.dto.AppointmentDTO;
import lombok.*;
import org.jetbrains.annotations.NotNull;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientNotificationDTO {
    private String message;

    public ClientNotificationDTO(@NotNull AppointmentDTO appointment) {
        this.message = "Reminder: You have a training session tomorrow at " + appointment.getStartTime();
    }
}
