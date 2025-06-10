package com.example.demo.dto.notification;

import com.example.demo.dto.AppointmentDTO;
import lombok.*;
import org.jetbrains.annotations.NotNull;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientAppointmentReminderNotificationDTO {
    private String message;

    public ClientAppointmentReminderNotificationDTO(@NotNull AppointmentDTO appointment) {
        this.message = "Reminder: You have a training session tomorrow at " + appointment.getStartTime();
    }
}
