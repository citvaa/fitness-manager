package com.example.demo.service.notification;

import com.example.demo.dto.AppointmentDTO;
import com.example.demo.dto.PaymentDTO;
import com.example.demo.dto.gym.RoomOccupancyDTO;
import com.example.demo.model.user.Client;
import com.example.demo.model.user.Trainer;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.util.List;

public interface NotificationService {

    void sendTrainerAssignmentNotification(Trainer trainer, AppointmentDTO appointmentDTO) throws JsonProcessingException;

    void sendTrainerScheduleNotification(Trainer trainer, List<AppointmentDTO> appointments);

    void sendClientAppointmentReminderNotification(Client client, AppointmentDTO appointment);

    void sendClientUpcomingAppointmentNotification(Client client, AppointmentDTO appointment);

    /** Client-facing confirmation that a payment was recorded by a manager - respects
     * {@code NotificationPreference} like the other per-client notifications above. */
    void sendPaymentConfirmationNotification(Client client, PaymentDTO payment);

    /** Broadcasts a manager-facing alert (new client self-booking, trainer self-assign) to every
     * manager - see {@code ManagerAlertNotificationDTO} for why this is push-only/no-preference. */
    void sendManagerAlert(String message);

    /**
     * Pushes the full current occupancy snapshot (every room) to {@code /topic/gym/occupancy} -
     * see AGENTS.md ("Upgrade: service layer decisions") for the message-format rationale. Sent
     * both on check-in/check-out events and periodically by {@code OccupancyScheduler}.
     */
    void sendGymOccupancyUpdate(List<RoomOccupancyDTO> occupancies);
}
