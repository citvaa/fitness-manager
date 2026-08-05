package com.example.demo.service.impl.ai;

import com.example.demo.exception.ApiException;
import com.example.demo.service.ai.ClaudeService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ClaudeServiceImpl implements ClaudeService {
    private final RestClient.Builder restClientBuilder;

    @Value("${app.anthropic.api-key:}")
    private String apiKey;
    @Value("${app.anthropic.model:claude-haiku-4-5-20251001}")
    private String model;
    @Value("${app.anthropic.max-tokens:500}")
    private int maxTokens;

    public String generate(String systemPrompt, String userPrompt) {
        if (apiKey == null || apiKey.isBlank()) throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "ANTHROPIC_API_KEY is not configured");
        try {
            JsonNode response = restClientBuilder.clone().baseUrl("https://api.anthropic.com").build().post()
                    .uri("/v1/messages")
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", "2023-06-01")
                    .body(Map.of(
                            "model", model,
                            "max_tokens", maxTokens,
                            "system", systemPrompt,
                            "messages", List.of(Map.of("role", "user", "content", userPrompt))))
                    .retrieve().body(JsonNode.class);
            if (response == null || !response.path("content").isArray()) throw new ApiException(HttpStatus.BAD_GATEWAY, "Claude API returned an empty response");
            StringBuilder text = new StringBuilder();
            response.path("content").forEach(block -> { if ("text".equals(block.path("type").asText())) text.append(block.path("text").asText()); });
            if (text.isEmpty()) throw new ApiException(HttpStatus.BAD_GATEWAY, "Claude API returned no text");
            return text.toString().trim();
        } catch (RestClientResponseException exception) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Claude API request failed with status " + exception.getStatusCode().value());
        }
    }

    public String model() { return model; }
}
