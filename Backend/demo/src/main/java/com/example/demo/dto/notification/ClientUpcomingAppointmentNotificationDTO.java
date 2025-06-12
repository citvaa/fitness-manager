package com.example.demo.dto.notification;

import com.example.demo.dto.AppointmentDTO;
import lombok.*;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientUpcomingAppointmentNotificationDTO {
    private String message;

    @Contract(pure = true)
    public ClientUpcomingAppointmentNotificationDTO(@NotNull AppointmentDTO appointment) {
        this.message = "Reminder: You have a training session at " + appointment.getStartTime();
    }
}
