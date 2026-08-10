package com.example.demo.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Minimal global exception handler - see AGENTS.md "Known issues"/"Upgrade: Faza 6 decisions"
 * (continued). Before this, any unhandled service-layer exception fell through to Spring Boot's
 * default error response, which drops the exception message entirely and returns a bare
 * {"status":500} with no explanation - the caller had no way to tell "you asked for something
 * invalid" from "the server is broken". This does not attempt to be a complete, codebase-wide
 * exception taxonomy (that's a bigger behavior change than this fix's scope) - it handles
 * exactly the two exception types this fix was asked to cover, plus the
 * {@link AccessDeniedException} carve-out below.
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
     * {@link AccessDeniedException} is a {@link RuntimeException}, so without this explicit,
     * more-specific handler it would silently fall into {@link #handleRuntimeException} below
     * and be reported as a 400 instead of a 403 - a real regression caught during this session's
     * fresh-volume verification (see AGENTS.md "Known issues"): the ownership check in
     * TrainerScheduleServiceImpl.deleteSchedule (and every other AccessDeniedException-throwing
     * check added across Faza 6/7 - e.g. TrainerClientAccessGuard) had been verified as a 403
     * response *before* the bare-RuntimeException handler below existed; adding that handler
     * later silently downgraded all of them to 400 with no test or manual check catching it,
     * since none of the earlier verification re-ran after this handler was added.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponse(ex.getMessage()));
    }

    /**
     * {@link DataIntegrityViolationException} wraps every unique/FK constraint violation the
     * database itself rejects (e.g. a duplicate email slipping past an application-level
     * pre-check under a race, or any other unique-constraint clash). Without this explicit,
     * more-specific handler it fell into {@link #handleRuntimeException} below - since
     * DataIntegrityViolationException is NOT a RuntimeException subtype directly reachable by
     * name matching there, Spring's default error handling took over instead and returned the
     * raw JDBC/Hibernate exception message straight to the client (constraint name, table name,
     * SQL state - meaningless and leaky to an end user). Mapped to 409 (Conflict) with a generic,
     * user-facing message rather than surfacing the underlying SQL error.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        log.warn("Data integrity violation mapped to 409: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("Već postoji unos sa ovim podacima"));
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
