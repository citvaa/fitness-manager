package com.example.demo.config.ai;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the Anthropic (Claude) SDK client used by the AI manager-insights and client-progress-
 * narrative features (see AGENTS.md - "Upgrade: service layer decisions"). The key is bound via
 * the standard Spring {@code app.anthropic.api-key} property ({@code ${ANTHROPIC_API_KEY:}} in
 * application.yaml) and passed explicitly to the client builder - not {@code fromEnv()}, which
 * reads the raw OS environment variable directly and would silently bypass Spring entirely (see
 * AGENTS.md - "Upgrade: dev-tooling decisions" for why that broke local `.env`-file loading for
 * this one variable specifically, unlike MAIL_/JWT_SECRET).
 * <p>
 * The key is intentionally not validated eagerly here: an unset key must not crash app startup
 * (the rest of the app has nothing to do with the AI features), it should only fail the specific
 * request that needs it. {@link com.example.demo.service.impl.ai.ClaudeInsightServiceImpl}
 * surfaces a clear error at call time instead.
 */
@Slf4j
@Configuration
public class AnthropicConfig {

    @Value("${app.anthropic.api-key:}")
    private String apiKey;

    @PostConstruct
    public void logKeyPresence() {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("⚠️ ANTHROPIC_API_KEY is not set - AI manager-insights and client-progress-narrative endpoints will fail until it is configured.");
        }
    }

    @Bean
    public AnthropicClient anthropicClient() {
        return AnthropicOkHttpClient.builder().apiKey(apiKey).build();
    }
}
