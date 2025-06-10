package com.example.demo.service;

import com.example.demo.dto.AppointmentDTO;
import com.example.demo.model.Client;
import com.example.demo.model.Trainer;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.util.List;

public interface NotificationService {

    void sendTrainerAssignmentNotification(Integer trainerId, AppointmentDTO appointmentDTO) throws JsonProcessingException;

    void sendTrainerScheduleNotification(Trainer trainer, List<AppointmentDTO> appointments);

    void sendClientAppointmentReminderNotification(Client client, AppointmentDTO appointment);

    void sendClientUpcomingAppointmentNotification(Client client, AppointmentDTO appointment);
}
