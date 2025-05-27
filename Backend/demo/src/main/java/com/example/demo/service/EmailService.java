package com.example.demo.service;

import com.example.demo.service.params.request.Email.ActivationEmailData;
import com.example.demo.service.params.request.Email.ForgetPasswordEmailData;

public interface EmailService {

    void sendActivationEmail(String recipient, ActivationEmailData emailData);

    void sendResetPasswordEmail(String recipient, ForgetPasswordEmailData emailData);

    String generateEmailContent(ActivationEmailData emailData);
}
