package com.example.demo.service.impl.notification.email;

import com.example.demo.service.notification.email.AsyncEmailService;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class AsyncEmailServiceImpl implements AsyncEmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromAddress;

    @Async
    public void sendHtmlEmail(String recipient, String subject, String htmlContent) {
        log.info("Sending email in thread: {}",Thread.currentThread().getName());
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(recipient);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            helper.setFrom(fromAddress);

            mailSender.send(message);
            log.info("Email sent to {}", recipient);
        } catch (Exception e) {
            log.error("Error trying to send email");
        }
    }

    @Async
    public void sendEmail(String to, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        message.setFrom(fromAddress);

        mailSender.send(message);
    }

}
