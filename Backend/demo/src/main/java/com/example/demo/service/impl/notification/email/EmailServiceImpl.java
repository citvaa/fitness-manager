package com.example.demo.service.impl.notification.email;

import com.example.demo.dto.AppointmentDTO;
import com.example.demo.dto.PaymentDTO;
import com.example.demo.dto.summary.ClientSummaryDTO;
import com.example.demo.service.notification.email.AsyncEmailService;
import com.example.demo.service.notification.email.EmailService;
import com.example.demo.service.params.request.email.ActivationEmailData;
import com.example.demo.service.params.request.email.ForgetPasswordEmailData;
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

    public void sendClientAppointmentReminderEmail(String clientEmail, @NotNull AppointmentDTO appointment) {
        String subject = "Podsetnik za termin";
        String body = "Poštovani, imate zakazan termin treninga sutra u " + appointment.getStartTime();
        asyncEmailService.sendEmail(clientEmail, subject, body);
    }

    public void sendTrainerScheduleEmail(String trainerEmail, @NotNull List<AppointmentDTO> appointments) {
        String subject = "Vaš raspored za sutra";
        StringBuilder body = new StringBuilder("Poštovani, evo vašeg rasporeda za sutra:\n\n");

        for (AppointmentDTO appointment : appointments) {
            body.append("⏰ ").append(appointment.getStartTime())
                    .append(" - ").append(appointment.getEndTime())
                    .append(" | Klijenti: ");

            Set<ClientSummaryDTO> clients = appointment.getClients();
            List<String> clientNames = clients.stream()
                    .map(ClientSummaryDTO::getEmail)
                    .toList();

            body.append(String.join(", ", clientNames)).append("\n");
        }

        asyncEmailService.sendEmail(trainerEmail, subject, body.toString());
    }

    public void sendTrainerAssignmentEmail(String trainerEmail, @NotNull AppointmentDTO appointment) {
        String subject = "Dodeljeni ste novom terminu";
        String body = "Poštovani, dodeljeni ste novom terminu treninga " + appointment.getDate()
                + " u " + appointment.getStartTime() + ".";
        asyncEmailService.sendEmail(trainerEmail, subject, body);
    }

    public void sendClientUpcomingAppointmentEmail(String clientEmail, @NotNull AppointmentDTO appointment) {
        String subject = "Termin uskoro počinje";
        String body = "Poštovani, imate termin treninga u " + appointment.getStartTime() + ".";
        asyncEmailService.sendEmail(clientEmail, subject, body);
    }

    public void sendPaymentConfirmationEmail(String clientEmail, @NotNull PaymentDTO payment) {
        String subject = "Potvrda uplate";
        String body = "Poštovani, evidentirali smo Vašu uplatu od " + payment.getPaidAppointments()
                + " termina (" + payment.getSession().getType() + ") na dan " + payment.getPaymentDate() + ".";
        asyncEmailService.sendEmail(clientEmail, subject, body);
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
