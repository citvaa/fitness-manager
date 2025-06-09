package com.example.demo.service;

import com.example.demo.dto.AppointmentDTO;
import com.example.demo.model.Client;
import com.example.demo.model.Trainer;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.util.List;

public interface NotificationService {

    void sendTrainerNotification(Integer trainerId, AppointmentDTO appointmentDTO) throws JsonProcessingException;

    void sendTrainerNotification(Trainer trainer, List<AppointmentDTO> appointments);

    void sendClientNotification(Client client, AppointmentDTO appointment);

    void sendClientNotificationTrainingReminder(Client client, AppointmentDTO appointment);
}
