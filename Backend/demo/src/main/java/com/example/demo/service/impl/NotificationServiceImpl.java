package com.example.demo.service.impl;

import com.example.demo.dto.AppointmentDTO;
import com.example.demo.dto.notification.ClientNotificationDTO;
import com.example.demo.dto.notification.TrainerNotificationDTO;
import com.example.demo.model.User;
import com.example.demo.service.EmailService;
import com.example.demo.service.NotificationService;
import com.example.demo.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class NotificationServiceImpl implements NotificationService {

    private final SimpMessagingTemplate messagingTemplate;
    private final EmailService emailService;

    public void sendTrainerNotification(Integer trainerId, AppointmentDTO appointmentDTO) {
        String jsonPayload = JsonUtil.convertToJson(appointmentDTO);

        messagingTemplate.convertAndSend("/topic/trainer" + trainerId, jsonPayload);
    }

    public void sendTrainerNotification(@NotNull User trainer, List<AppointmentDTO> appointments) {
        TrainerNotificationDTO notificationDTO = new TrainerNotificationDTO(appointments);
        String jsonPayload = JsonUtil.convertToJson(notificationDTO);

        switch (trainer.getNotificationPreference()) {
            case BOTH -> {
                emailService.sendTrainerScheduleEmail(trainer.getEmail(), appointments);
                messagingTemplate.convertAndSend("/topic/trainer" + trainer.getId(), jsonPayload);
            }
            case EMAIL -> emailService.sendTrainerScheduleEmail(trainer.getEmail(), appointments);
            case PUSH -> messagingTemplate.convertAndSend("/topic/trainer" + trainer.getId(), jsonPayload);
        }
    }

    public void sendClientNotification(@NotNull User client, AppointmentDTO appointment) {
        ClientNotificationDTO notificationDTO = new ClientNotificationDTO(appointment);
        String jsonPayload = JsonUtil.convertToJson(notificationDTO);

        switch (client.getNotificationPreference()) {
            case BOTH -> {
                emailService.sendClientNotificationEmail(client.getEmail(), appointment);
                messagingTemplate.convertAndSend("/topic/client" + client.getId(), jsonPayload);
            }
            case EMAIL -> emailService.sendClientNotificationEmail(client.getEmail(), appointment);
            case PUSH -> messagingTemplate.convertAndSend("/topic/client" + client.getId(), jsonPayload);
        }
    }
}
