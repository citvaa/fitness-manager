package com.example.demo.service.impl;

import com.example.demo.dto.AppointmentDTO;
import com.example.demo.dto.notification.TrainerAssignmentNotificationDTO;
import com.example.demo.dto.notification.ClientAppointmentReminderNotificationDTO;
import com.example.demo.dto.notification.TrainerScheduleNotificationDTO;
import com.example.demo.dto.notification.ClientUpcomingAppointmentNotificationDTO;
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
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class NotificationServiceImpl implements NotificationService {

    private final SimpMessagingTemplate messagingTemplate;
    private final EmailService emailService;
    private final UserRepository userRepository;

    public void sendTrainerAssignmentNotification(Integer trainerId, AppointmentDTO appointmentDTO) {
        String jsonPayload = JsonUtil.convertToJson(new TrainerAssignmentNotificationDTO(appointmentDTO));

        messagingTemplate.convertAndSend("/topic/trainer" + trainerId, jsonPayload);
    }

    public void sendTrainerScheduleNotification(@NotNull Trainer trainer, List<AppointmentDTO> appointments) {
        String jsonPayload = JsonUtil.convertToJson(new TrainerScheduleNotificationDTO(appointments));

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

    public void sendClientAppointmentReminderNotification(@NotNull Client client, AppointmentDTO appointment) {
        String jsonPayload = JsonUtil.convertToJson(new ClientAppointmentReminderNotificationDTO(appointment));

        User user = userRepository.findById(client.getUser().getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        switch (user.getNotificationPreference()) {
            case BOTH -> {
                emailService.sendClientAppointmentReminderEmail(client.getUser().getEmail(), appointment);
                messagingTemplate.convertAndSend("/topic/client" + client.getId(), jsonPayload);
            }
            case EMAIL -> emailService.sendClientAppointmentReminderEmail(client.getUser().getEmail(), appointment);
            case PUSH -> messagingTemplate.convertAndSend("/topic/client" + client.getId(), jsonPayload);
        }
    }

    public void sendClientUpcomingAppointmentNotification(@NotNull Client client, AppointmentDTO appointment) {
        String jsonPayload = JsonUtil.convertToJson(new ClientUpcomingAppointmentNotificationDTO(appointment));
        messagingTemplate.convertAndSend("/topic/client" + client.getId(), jsonPayload);
    }
}
