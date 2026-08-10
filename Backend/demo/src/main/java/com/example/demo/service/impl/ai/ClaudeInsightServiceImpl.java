package com.example.demo.service.impl.ai;

import com.anthropic.client.AnthropicClient;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.example.demo.service.ai.ClaudeInsightService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

/**
 * See AGENTS.md - "Upgrade: service layer decisions" for why {@code claude-haiku-4-5} was picked
 * for this feature: manager insights and client-progress narratives are short, well-scoped
 * summarization/recommendation calls over data this service has already aggregated (not an
 * open-ended agentic task), and they are triggered relatively often (page loads, gated only by
 * the Redis cache below) - the cheapest/fastest current Claude model is the right fit rather than
 * a frontier reasoning model.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClaudeInsightServiceImpl implements ClaudeInsightService {

    private static final String MODEL = "claude-haiku-4-5";
    private static final long MAX_TOKENS = 1024L;

    private final AnthropicClient anthropicClient;

    @Value("${app.anthropic.api-key:}")
    private String apiKey;

    @Override
    public String generate(String systemPrompt, String userPrompt) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("ANTHROPIC_API_KEY is not set - cannot call the Claude API for AI insights.");
        }

        MessageCreateParams params = MessageCreateParams.builder()
                .model(MODEL)
                .maxTokens(MAX_TOKENS)
                .system(systemPrompt)
                .addUserMessage(userPrompt)
                .build();

        Message response;
        try {
            response = anthropicClient.messages().create(params);
        } catch (Exception e) {
            log.error("❌ Claude API call failed: {}", e.getMessage());
            throw new IllegalStateException("Claude API call failed: " + e.getMessage(), e);
        }

        String text = response.content().stream()
                .flatMap(block -> block.text().stream())
                .map(com.anthropic.models.messages.TextBlock::text)
                .collect(Collectors.joining("\n"));

        if (text.isBlank()) {
            log.warn("⚠️ Claude response had no text content (stop_reason={})", response.stopReason());
            return "";
        }

        return text;
    }
}
