package com.example.demo.service.impl;

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

import java.util.Locale;

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
