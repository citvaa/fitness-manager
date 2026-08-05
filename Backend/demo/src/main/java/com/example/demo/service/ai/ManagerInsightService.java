package com.example.demo.service.ai;

import com.example.demo.service.params.response.ai.AiInsightResponse;

public interface ManagerInsightService {
    AiInsightResponse getInsights(boolean forceRegeneration);
}
