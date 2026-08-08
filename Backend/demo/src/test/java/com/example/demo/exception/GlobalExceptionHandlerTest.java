package com.example.demo.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link GlobalExceptionHandler} - added in Faza 6 specifically to fix the
 * observed bug where {@code TrainerScheduleServiceImpl}'s validation exceptions (gym-hours
 * checks, overlap checks, etc.) surfaced as a content-less 500. See AGENTS.md "Upgrade: Faza 6
 * decisions (continued, part 2)" / "Known issues".
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void illegalArgumentException_mapsToBadRequestWithMessage() {
        ResponseEntity<ErrorResponse> response = handler.handleIllegalArgument(
                new IllegalArgumentException("Start time is after end time"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Start time is after end time");
    }

    @Test
    void bareRuntimeException_alsoMapsToBadRequestWithMessage() {
        // e.g. TrainerScheduleServiceImpl.validateGymHours - "No gym schedule found for ..." -
        // previously fell through to a bare {"status":500} with the message dropped entirely.
        ResponseEntity<ErrorResponse> response = handler.handleRuntimeException(
                new RuntimeException("No gym schedule found for 2026-08-09"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("No gym schedule found for 2026-08-09");
    }
}
