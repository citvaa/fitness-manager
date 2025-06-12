package com.example.demo.service.notification.email;

public interface AsyncEmailService {
    void sendHtmlEmail(String recipient, String subject, String htmlContent);

    void sendEmail(String to, String subject, String body);
}
