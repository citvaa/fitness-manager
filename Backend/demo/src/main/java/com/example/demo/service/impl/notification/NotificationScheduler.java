package com.example.demo.service.impl.notification;

import com.example.demo.dto.AppointmentDTO;
import com.example.demo.mapper.AppointmentMapper;
import com.example.demo.mapper.ClientMapper;
import com.example.demo.mapper.TrainerMapper;
import com.example.demo.model.Appointment;
import com.example.demo.model.user.Client;
import com.example.demo.model.user.Trainer;
import com.example.demo.service.AppointmentService;
import com.example.demo.service.user.ClientService;
import com.example.demo.service.notification.NotificationService;
import com.example.demo.service.user.TrainerService;
import lombok.RequiredArgsConstructor;
import org.hibernate.Hibernate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationScheduler {

    private final TrainerService trainerService;
    private final AppointmentService appointmentService;
    private final NotificationService notificationService;
    private final TrainerMapper trainerMapper;
    private final ClientService clientService;
    private final ClientMapper clientMapper;
    private final AppointmentMapper appointmentMapper;

    @Scheduled(cron = "0 0 20 * * ?")
    public void sendDailyTrainerNotifications() {
        System.out.println("🔥 Sending daily trainer notifications...");

        List<Trainer> trainers = trainerMapper.toEntity(trainerService.getAll());
        for (Trainer trainer : trainers) {
            List<AppointmentDTO> appointments = appointmentService.getAppointmentsForTrainer(trainer.getId(), LocalDate.now().plusDays(1));
            notificationService.sendTrainerScheduleNotification(trainer, appointments);
        }

        System.out.println("✅ Trainer notifications sent!");
    }

    @Scheduled(cron = "0 0 20 * * ?")
    public void sendDailyClientNotifications() {
        System.out.println("🔥 Sending daily client notifications...");

        List<Client> clients = clientMapper.toEntity(clientService.getAll());
        for (Client client : clients) {
            Optional<AppointmentDTO> appointments = appointmentService.getAppointmentForClient(client.getId(), LocalDate.now().plusDays(1));
            appointments.ifPresent(appointmentDTO -> notificationService.sendClientAppointmentReminderNotification(client, appointmentDTO));
        }

        System.out.println("✅ Client notifications sent!");
    }

    @Scheduled(cron = "0 0 * * * ?")
    public void sendUpcomingAppointmentNotifications() {
        System.out.println("🔥 Sending upcoming training notifications...");

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneHourLater = now.plusHours(1);

        List<Appointment> upcomingAppointments = appointmentService.findAppointmentsStartingBetween(now.toLocalTime(), oneHourLater.toLocalTime(), now.toLocalDate());

        upcomingAppointments.forEach(appointment -> {
            Hibernate.initialize(appointment.getClientAppointments());
            appointment.getClientAppointments()
                    .forEach(clientAppointment -> {
                        if (clientAppointment.getClient() != null) {
                            Client client = clientAppointment.getClient();
                            notificationService.sendClientUpcomingAppointmentNotification(client, appointmentMapper.toDto(appointment));
                            System.out.println("✅ Sent reminder to client: " + client.getId());
                        } else {
                            System.err.println("❌ Client is null for appointment ID: " + appointment.getId());
                        }
                    });
        });
    }
}
