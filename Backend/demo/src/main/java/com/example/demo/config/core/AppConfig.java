package com.example.demo.config.core;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app")
@Getter
@Setter
public class AppConfig {
    private Integer registrationKeyValidityMinutes;
    private Integer resetKeyValidityMinutes;
    private Frontend frontend = new Frontend();

    @Getter
    @Setter
    public static class Frontend {
        /** Base origin the activation/reset-password emails link back to - see app.frontend.url
         * in application.yaml. Configurable (env var FRONTEND_URL) rather than hardcoded, since
         * this differs between local dev and a real deployment. */
        private String url;
    }
}
