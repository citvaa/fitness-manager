package com.example.demo.service;

import com.example.demo.dto.AppointmentDTO;
import com.example.demo.model.User;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.util.List;

public interface NotificationService {

    void sendTrainerNotification(Integer trainerId, AppointmentDTO appointmentDTO) throws JsonProcessingException;

    void sendTrainerNotification(User trainer, List<AppointmentDTO> appointments);

    void sendClientNotification(User client, AppointmentDTO appointment);
}
