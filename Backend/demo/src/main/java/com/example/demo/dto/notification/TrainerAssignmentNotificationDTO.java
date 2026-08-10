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
        this.message = "Obaveštenje: dodeljeni ste novom terminu treninga " + appointment.getDate() + " u " + appointment.getStartTime();
    }
}
