package com.example.demo.dto.notification;

import com.example.demo.dto.AppointmentDTO;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrainerNotificationDTO {
    private LocalDate date;
    private List<AppointmentDTO> appointments;

    public TrainerNotificationDTO(List<AppointmentDTO> appointments) {
        this.date = LocalDate.now().plusDays(1);
        this.appointments = appointments;
    }
}
