package com.example.demo.service.ai;

/**
 * Thin wrapper around the Anthropic Messages API used by the manager-insights and client-
 * progress-narrative features. See AGENTS.md - "Upgrade: service layer decisions" for the model
 * choice and caching rationale.
 */
public interface ClaudeInsightService {

    /**
     * Sends a single-turn request (system prompt + user prompt) and returns Claude's plain-text
     * response. No conversation history, no tools - these features are one-shot summarization/
     * recommendation calls, not agents.
     */
    String generate(String systemPrompt, String userPrompt);
}
