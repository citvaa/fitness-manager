package com.example.demo.service;

import com.example.demo.dto.SessionDTO;

import java.util.List;

public interface SessionService {
    /** Session *types* are seeded rows only (see AGENTS.md domain model), never created via the API. */
    List<SessionDTO> getAll();
}
