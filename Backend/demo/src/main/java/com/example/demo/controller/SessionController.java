package com.example.demo.controller;

import com.example.demo.annotation.RoleRequired;
import com.example.demo.dto.SessionDTO;
import com.example.demo.service.SessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Minimal GET added alongside the Phase 6 payments screen - the MANAGER "new payment" form needs
 * to let the manager pick a session type/id, and no endpoint existed to list the seeded Session
 * rows at all (see AGENTS.md domain model: sessions are seeded-only, never created via the API).
 * MANAGER-only since this is only consumed by the payment-creation form for now.
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/session")
public class SessionController {

    private final SessionService sessionService;

    @RoleRequired("MANAGER")
    @GetMapping
    public ResponseEntity<List<SessionDTO>> getAll() {
        return ResponseEntity.ok(sessionService.getAll());
    }
}
