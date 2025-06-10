package com.example.demo.dto.notification;

import com.example.demo.dto.AppointmentDTO;
import lombok.*;
import org.jetbrains.annotations.NotNull;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class TrainerAssignmentNotificationDTO {
    private String message;

    public TrainerAssignmentNotificationDTO(@NotNull AppointmentDTO appointment) {
        this.message = "Update: You have been assigned for a new training session on " + appointment.getDate() + " at " + appointment.getStartTime();
    }
}
