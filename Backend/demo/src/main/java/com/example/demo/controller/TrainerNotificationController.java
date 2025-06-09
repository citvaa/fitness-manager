package com.example.demo.controller;

import com.example.demo.dto.notification.TrainerAppointmentNotificationDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class TrainerNotificationController {

    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("send-notification")
    public void sendNotification(@Payload TrainerAppointmentNotificationDTO notification) {
        messagingTemplate.convertAndSend("/topic/trainer" + notification.getAppointment().getTrainer().getId(), notification);
    }
}
