package com.example.demo.service.ai;

public interface ClaudeService {
    String generate(String systemPrompt, String userPrompt);
    String model();
}
