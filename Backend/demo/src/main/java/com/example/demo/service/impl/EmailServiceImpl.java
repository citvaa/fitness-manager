package com.example.demo.service.impl;

import com.example.demo.dto.AppointmentDTO;
import com.example.demo.dto.summary.ClientSummaryDTO;
import com.example.demo.service.AsyncEmailService;
import com.example.demo.service.EmailService;
import com.example.demo.service.params.request.Email.ActivationEmailData;
import com.example.demo.service.params.request.Email.ForgetPasswordEmailData;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.List;
import java.util.Locale;
import java.util.Set;

@RequiredArgsConstructor
@Service
public class EmailServiceImpl implements EmailService {

    private final TemplateEngine templateEngine;
    private final AsyncEmailService asyncEmailService;

    @Value("${app.email.activation-template}")
    private String activationTemplatePath;

    @Value("${app.email.reset-password-template}")
    private String resetPasswordTemplatePath;

    public void sendActivationEmail(String recipient, @NotNull ActivationEmailData emailData) {
        String emailContent = generateEmailContent(emailData);
        asyncEmailService.sendHtmlEmail(recipient, "Aktivacija naloga", emailContent);
    }

    public void sendResetPasswordEmail(String recipient, @NotNull ForgetPasswordEmailData emailData) {
        String emailContent = generateEmailContent(emailData);
        asyncEmailService.sendHtmlEmail(recipient, "Zaboravljena lozinka", emailContent);
    }

    public void sendClientNotificationEmail(String clientEmail, @NotNull AppointmentDTO appointment) {
        String subject = "Training Reminder";
        String body = "Hello, you have a training session scheduled tomorrow at " + appointment.getStartTime();
        asyncEmailService.sendEmail(clientEmail, subject, body);
    }

    public void sendTrainerScheduleEmail(String trainerEmail, @NotNull List<AppointmentDTO> appointments) {
        String subject = "Your Schedule for Tomorrow";
        StringBuilder body = new StringBuilder("Hello, here is your schedule for tomorrow:\n\n");

        for (AppointmentDTO appointment : appointments) {
            body.append("⏰ ").append(appointment.getStartTime())
                    .append(" - ").append(appointment.getEndTime())
                    .append(" | Clients: ");

            Set<ClientSummaryDTO> clients = appointment.getClients();
            List<String> clientNames = clients.stream()
                    .map(ClientSummaryDTO::getEmail)
                    .toList();

            body.append(String.join(", ", clientNames)).append("\n");
        }

        asyncEmailService.sendEmail(trainerEmail, subject, body.toString());
    }




    private String generateEmailContent(@NotNull ActivationEmailData emailData) {
        Context context = new Context(Locale.getDefault());
        context.setVariables(emailData.toMap());

        return templateEngine.process(activationTemplatePath, context);
    }

    private String generateEmailContent(@NotNull ForgetPasswordEmailData emailData) {
        Context context = new Context(Locale.getDefault());
        context.setVariables(emailData.toMap());

        return templateEngine.process(resetPasswordTemplatePath, context);
    }
}
