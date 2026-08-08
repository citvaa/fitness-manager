package com.example.demo.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Minimal global exception handler - see AGENTS.md "Known issues"/"Upgrade: Faza 6 decisions"
 * (continued). Before this, any unhandled service-layer exception fell through to Spring Boot's
 * default error response, which drops the exception message entirely and returns a bare
 * {"status":500} with no explanation - the caller had no way to tell "you asked for something
 * invalid" from "the server is broken". This does not attempt to be a complete, codebase-wide
 * exception taxonomy (that's a bigger behavior change than this fix's scope) - it handles
 * exactly the two exception types this fix was asked to cover.
 *
 * {@link IllegalStateException} is deliberately NOT handled here: RoomCheckInController already
 * catches it locally and returns 409 with a message (see "Upgrade: service layer decisions") -
 * that local catch happens before the exception would ever reach this advice, so adding a
 * global handler for it would be redundant, not an omission.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Rejected request: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(ex.getMessage()));
    }

    /**
     * Catch-all for bare RuntimeException (e.g. TrainerScheduleServiceImpl's
     * "No gym schedule found for ..."/"Trainer not found" - existing code that throws a plain
     * RuntimeException instead of a purpose-built exception type). Note this also converts other
     * unhandled RuntimeExceptions (including genuine bugs, e.g. a NullPointerException) into a
     * 400 with that exception's message rather than a 500 - an accepted trade for this fix's
     * minimal scope, not a deliberate statement that every RuntimeException is the caller's
     * fault. A more complete pass would introduce purpose-built exception types (e.g. a real
     * "not found" -> 404 mapping for jakarta.persistence.EntityNotFoundException, currently swept
     * into this same 400 handler) - left as a future improvement, out of scope here.
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException ex) {
        log.warn("Unhandled runtime exception mapped to 400: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(ex.getMessage()));
    }
}
