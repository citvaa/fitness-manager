package com.example.demo.service;

import com.example.demo.model.Appointment;
import com.example.demo.service.params.request.Email.ActivationEmailData;
import com.example.demo.service.params.request.Email.ForgetPasswordEmailData;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface EmailService {

    void sendActivationEmail(String recipient, ActivationEmailData emailData);

    void sendResetPasswordEmail(String recipient, ForgetPasswordEmailData emailData);

    void sendClientNotificationEmail(String clientEmail, @NotNull Appointment appointment);

    void sendTrainerScheduleEmail(String trainerEmail, @NotNull List<Appointment> appointments);
}
