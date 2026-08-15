package com.example.demo.service.ai;

import com.example.demo.service.params.response.ai.ManagerInsightResponse;

public interface ManagerInsightService {
    ManagerInsightResponse getInsights(boolean forceRegeneration);
}
