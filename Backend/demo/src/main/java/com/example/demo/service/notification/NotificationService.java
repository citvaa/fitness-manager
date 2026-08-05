package com.example.demo.service.notification;

import com.example.demo.dto.AppointmentDTO;
import com.example.demo.dto.gym.RoomOccupancyDTO;
import com.example.demo.model.user.Client;
import com.example.demo.model.user.Trainer;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.util.List;

public interface NotificationService {

    void sendTrainerAssignmentNotification(Integer trainerId, AppointmentDTO appointmentDTO) throws JsonProcessingException;

    void sendTrainerScheduleNotification(Trainer trainer, List<AppointmentDTO> appointments);

    void sendClientAppointmentReminderNotification(Client client, AppointmentDTO appointment);

    void sendClientUpcomingAppointmentNotification(Client client, AppointmentDTO appointment);

    /**
     * Pushes the full current occupancy snapshot (every room) to {@code /topic/gym/occupancy} -
     * see AGENTS.md ("Upgrade: service layer decisions") for the message-format rationale. Sent
     * both on check-in/check-out events and periodically by {@code OccupancyScheduler}.
     */
    void sendGymOccupancyUpdate(List<RoomOccupancyDTO> occupancies);
}
