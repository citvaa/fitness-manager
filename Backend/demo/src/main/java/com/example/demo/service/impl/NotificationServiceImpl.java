package com.example.demo.service.impl;

import com.example.demo.dto.AppointmentDTO;
import com.example.demo.dto.notification.ClientNotificationDTO;
import com.example.demo.dto.notification.TrainerNotificationDTO;
import com.example.demo.dto.notification.TrainingReminderDTO;
import com.example.demo.model.Client;
import com.example.demo.model.Trainer;
import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
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
    private final UserRepository userRepository;

    public void sendTrainerNotification(Integer trainerId, AppointmentDTO appointmentDTO) {
        String jsonPayload = JsonUtil.convertToJson(appointmentDTO);

        messagingTemplate.convertAndSend("/topic/trainer" + trainerId, jsonPayload);
    }

    public void sendTrainerNotification(@NotNull Trainer trainer, List<AppointmentDTO> appointments) {
        TrainerNotificationDTO notificationDTO = new TrainerNotificationDTO(appointments);
        String jsonPayload = JsonUtil.convertToJson(notificationDTO);

        User user = userRepository.findById(trainer.getUser().getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        switch (user.getNotificationPreference()) {
            case BOTH -> {
                emailService.sendTrainerScheduleEmail(trainer.getUser().getEmail(), appointments);
                messagingTemplate.convertAndSend("/topic/trainer" + trainer.getId(), jsonPayload);
                System.out.println("✅ Email & WebSocket notification sent!");
            }
            case EMAIL -> {
                emailService.sendTrainerScheduleEmail(trainer.getUser().getEmail(), appointments);
                System.out.println("✅ Email notification sent!");
            }
            case PUSH -> {
                messagingTemplate.convertAndSend("/topic/trainer" + trainer.getId(), jsonPayload);
                System.out.println("✅ WebSocket notification sent!");
            }
        }
    }

    public void sendClientNotification(@NotNull Client client, AppointmentDTO appointment) {
        ClientNotificationDTO notificationDTO = new ClientNotificationDTO(appointment);
        String jsonPayload = JsonUtil.convertToJson(notificationDTO);

        User user = userRepository.findById(client.getUser().getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        switch (user.getNotificationPreference()) {
            case BOTH -> {
                emailService.sendClientNotificationEmail(client.getUser().getEmail(), appointment);
                messagingTemplate.convertAndSend("/topic/client" + client.getId(), jsonPayload);
            }
            case EMAIL -> emailService.sendClientNotificationEmail(client.getUser().getEmail(), appointment);
            case PUSH -> messagingTemplate.convertAndSend("/topic/client" + client.getId(), jsonPayload);
        }
    }

    public void sendClientNotificationTrainingReminder(@NotNull Client client, AppointmentDTO appointment) {
        String jsonPayload = JsonUtil.convertToJson(new TrainingReminderDTO(appointment));
        messagingTemplate.convertAndSend("/topic/client" + client.getId(), jsonPayload);
    }
}
