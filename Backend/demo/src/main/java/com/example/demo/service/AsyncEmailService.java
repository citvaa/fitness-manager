package com.example.demo.service;

public interface AsyncEmailService {
    void sendHtmlEmail(String recipient, String subject, String htmlContent);
}
