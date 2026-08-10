package com.example.demo.service.impl.notification.email;

import com.example.demo.service.notification.email.AsyncEmailService;
import com.example.demo.service.params.request.email.ActivationEmailData;
import com.example.demo.service.params.request.email.ForgetPasswordEmailData;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

/**
 * Regression test for the activation/reset-password email link bug (see AGENTS.md
 * "Upgrade: manager-testing fixes") - the rendered email HTML used to hardcode
 * "https://nesto.com" instead of the real frontend origin. Renders the actual Thymeleaf
 * templates through the real {@link EmailServiceImpl} (only {@link AsyncEmailService}, the
 * actual SMTP send, is mocked) so this fails if the templates or the {@code app.frontend.url}
 * wiring regress.
 */
@SpringBootTest(properties = {"spring.profiles.active=dev"})
class EmailServiceImplTest {

    @Autowired
    private EmailServiceImpl emailService;

    @MockBean
    private AsyncEmailService asyncEmailService;

    @Test
    void activationEmail_linksToConfiguredFrontendOrigin_notTheOldHardcodedDomain() {
        ActivationEmailData data = ActivationEmailData.builder()
                .registrationKey("abc-123")
                .registrationKeyValidity("2026-01-01 10:00:00")
                .frontendUrl("http://localhost:5173")
                .build();

        emailService.sendActivationEmail("user@example.com", data);

        var captor = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(asyncEmailService).sendHtmlEmail(eq("user@example.com"), any(), captor.capture());
        String html = captor.getValue();

        assertThat(html).contains("http://localhost:5173/register/complete?registration_key=abc-123");
        assertThat(html).doesNotContain("nesto.com");
    }

    @Test
    void resetPasswordEmail_linksToConfiguredFrontendOrigin_notTheOldHardcodedDomain() {
        ForgetPasswordEmailData data = ForgetPasswordEmailData.builder()
                .resetKey("reset-xyz")
                .resetKeyValidity("2026-01-01 10:00:00")
                .frontendUrl("http://localhost:5173")
                .build();

        emailService.sendResetPasswordEmail("user@example.com", data);

        var captor = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(asyncEmailService).sendHtmlEmail(eq("user@example.com"), any(), captor.capture());
        String html = captor.getValue();

        assertThat(html).contains("http://localhost:5173/reset-password?reset_key=reset-xyz");
        assertThat(html).doesNotContain("nesto.com");
    }
}
