package com.example.demo.dto.notification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Broadcast-style alert for MANAGER accounts (a new client self-booking, a trainer self-assigning
 * to an open slot) - sent to every manager on /topic/manager with no per-recipient
 * NotificationPreference branching, same "public feed" rationale as the gym-occupancy broadcast:
 * there can be more than one MANAGER account and nothing in this codebase singles one out as
 * "the" recipient, so this is deliberately push-only rather than resolving/emailing every manager
 * individually. See AGENTS.md "Upgrade: notification decisions".
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ManagerAlertNotificationDTO {
    private String message;
}
