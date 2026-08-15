package com.example.demo.service.notification;

import com.example.demo.dto.AppointmentDTO;
import com.example.demo.dto.PaymentDTO;
import com.example.demo.model.user.Client;
import com.example.demo.model.user.Trainer;

import java.util.List;

public interface NotificationService {

    void sendTrainerAssignmentNotification(Integer trainerId, AppointmentDTO appointmentDTO);

    void sendTrainerScheduleNotification(Trainer trainer, List<AppointmentDTO> appointments);

    void sendClientAppointmentReminderNotification(Client client, AppointmentDTO appointment);

    void sendClientUpcomingAppointmentNotification(Client client, AppointmentDTO appointment);

    void sendManagerAlert(String message);

    void sendPaymentConfirmationNotification(Client client, PaymentDTO payment);
}
