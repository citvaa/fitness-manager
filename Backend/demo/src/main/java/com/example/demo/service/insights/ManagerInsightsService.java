package com.example.demo.service.insights;

import com.example.demo.dto.insights.ManagerInsightsDTO;

public interface ManagerInsightsService {

    /** Cached (30 min TTL - see RedisConfig). */
    ManagerInsightsDTO getInsights();

    /** Bypasses and refreshes the cache. */
    ManagerInsightsDTO refreshInsights();
}
