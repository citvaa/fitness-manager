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
        this.message = "Podsetnik: imate termin treninga sutra u " + appointment.getStartTime();
    }
}
