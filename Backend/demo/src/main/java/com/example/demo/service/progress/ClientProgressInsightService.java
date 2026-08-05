package com.example.demo.service.progress;

import com.example.demo.dto.progress.ClientProgressInsightDTO;

public interface ClientProgressInsightService {

    /** Cached per client (see RedisConfig.CLIENT_PROGRESS_INSIGHT_CACHE); evicted on new progress entries. */
    ClientProgressInsightDTO getSummary(Integer clientId);

    /** For the CLIENT role viewing their own narrative summary. */
    ClientProgressInsightDTO getMySummary();
}
