package com.example.demo.service.impl.gym;

import com.example.demo.service.gym.OccupancyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OccupancyScheduler {
    private final OccupancyService occupancyService;

    @Scheduled(fixedRateString = "${app.occupancy.broadcast-rate-ms:60000}")
    public void broadcast() {
        try {
            occupancyService.publishCurrentOccupancy();
            log.info("✅ Published gym occupancy snapshot");
        } catch (RuntimeException exception) {
            log.warn("❌ Could not publish gym occupancy snapshot: {}", exception.getMessage());
        }
    }
}
