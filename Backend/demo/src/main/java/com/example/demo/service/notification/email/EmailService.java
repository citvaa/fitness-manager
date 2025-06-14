package com.example.demo.service.notification.email;

import com.example.demo.dto.AppointmentDTO;
import com.example.demo.service.params.request.email.ActivationEmailData;
import com.example.demo.service.params.request.email.ForgetPasswordEmailData;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface EmailService {

    void sendActivationEmail(String recipient, ActivationEmailData emailData);

    void sendResetPasswordEmail(String recipient, ForgetPasswordEmailData emailData);

    void sendClientAppointmentReminderEmail(String clientEmail, @NotNull AppointmentDTO appointment);

    void sendTrainerScheduleEmail(String trainerEmail, @NotNull List<AppointmentDTO> appointments);
}
