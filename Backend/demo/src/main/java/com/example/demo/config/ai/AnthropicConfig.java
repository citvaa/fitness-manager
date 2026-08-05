package com.example.demo.config.ai;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the Anthropic (Claude) SDK client used by the AI manager-insights and client-progress-
 * narrative features (see AGENTS.md - "Upgrade: service layer decisions"). The client reads its
 * credentials from the ANTHROPIC_API_KEY environment variable ({@code fromEnv()}) - same pattern
 * as the MAIL_ and JWT_SECRET variables, see .env.example.
 * <p>
 * The key is intentionally not validated eagerly here: an unset key must not crash app startup
 * (the rest of the app has nothing to do with the AI features), it should only fail the specific
 * request that needs it. {@link com.example.demo.service.impl.ai.ClaudeInsightServiceImpl}
 * surfaces a clear error at call time instead.
 */
@Slf4j
@Configuration
public class AnthropicConfig {

    @PostConstruct
    public void logKeyPresence() {
        if (System.getenv("ANTHROPIC_API_KEY") == null || System.getenv("ANTHROPIC_API_KEY").isBlank()) {
            log.warn("⚠️ ANTHROPIC_API_KEY is not set - AI manager-insights and client-progress-narrative endpoints will fail until it is configured.");
        }
    }

    @Bean
    public AnthropicClient anthropicClient() {
        return AnthropicOkHttpClient.fromEnv();
    }
}
