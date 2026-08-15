package com.example.demo.service.impl.notification.email;

import org.junit.jupiter.api.Test;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ActivationEmailTemplateTest {

    @Test
    void activationLinkTargetsTheFrontendCompletionRoute() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode("HTML");

        Context context = new Context();
        context.setVariable("frontendUrl", "https://frontend.example");
        context.setVariable("registrationKey", "abc-123");
        context.setVariable("registrationKeyValidity", "tomorrow");

        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        String html = engine.process("email/activation-email", context);

        assertTrue(html.contains("https://frontend.example/complete-registration?key=abc-123"));
        assertFalse(html.contains("https://nesto.com"));
    }
}
