package com.example.demo.service.impl.notification;

import com.example.demo.service.gym.RoomCheckInService;
import com.example.demo.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Periodic occupancy broadcast, modelled on {@link NotificationScheduler}'s existing
 * {@code @Scheduled} pattern. Room-check-in-driven occupancy changes are already pushed
 * immediately by {@code RoomCheckInServiceImpl} on every check-in/check-out; this sweep exists
 * so that occupancy changes driven purely by appointment start/end times (no check-in/check-out
 * event involved) also reach the live floor-plan view, at latest a minute after they happen - see
 * AGENTS.md ("Upgrade: service layer decisions").
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OccupancyScheduler {

    private final RoomCheckInService roomCheckInService;
    private final NotificationService notificationService;

    @Scheduled(cron = "0 * * * * ?")
    public void broadcastOccupancy() {
        log.info("🔥 Broadcasting periodic gym occupancy update...");
        notificationService.sendGymOccupancyUpdate(roomCheckInService.getAllOccupancy());
        log.info("✅ Occupancy update sent!");
    }
}
