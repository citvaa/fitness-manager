package com.example.demo.service.impl;

import com.example.demo.service.EmailService;
import com.example.demo.service.params.request.Email.ActivationEmailData;
import com.example.demo.service.params.request.Email.ForgetPasswordEmailData;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Locale;

@RequiredArgsConstructor
@Service
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${spring.mail.username}")
    private String fromAddress;

    @Value("${app.email.activation-template}")
    private String activationTemplatePath;

    @Value("${app.email.reset-password-template}")
    private String resetPasswordTemplatePath;

    public void sendActivationEmail(String recipient, @NotNull ActivationEmailData emailData) {
        String emailContent = generateEmailContent(emailData);
        sendHtmlEmail(recipient, "Aktivacija naloga", emailContent);
    }

    public void sendResetPasswordEmail(String recipient, @NotNull ForgetPasswordEmailData emailData) {
        String emailContent = generateEmailContent(emailData);
        sendHtmlEmail(recipient, "Zaboravljena lozinka", emailContent);
    }

    public void sendHtmlEmail(String recipient, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(recipient);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            helper.setFrom(fromAddress);

            mailSender.send(message);
        } catch (Exception e) {
            log.error("Error trying to send email;");
        }

    }

    public String generateEmailContent(@NotNull ActivationEmailData emailData) {
        Context context = new Context(Locale.getDefault());
        context.setVariables(emailData.toMap());

        return templateEngine.process(activationTemplatePath, context);
    }

    public String generateEmailContent(@NotNull ForgetPasswordEmailData emailData) {
        Context context = new Context(Locale.getDefault());
        context.setVariables(emailData.toMap());

        return templateEngine.process(resetPasswordTemplatePath, context);
    }
}
