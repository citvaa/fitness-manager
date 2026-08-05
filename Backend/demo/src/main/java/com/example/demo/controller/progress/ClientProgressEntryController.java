package com.example.demo.controller.progress;

import com.example.demo.annotation.RoleRequired;
import com.example.demo.dto.progress.ClientProgressEntryDTO;
import com.example.demo.service.params.request.progress.CreateProgressEntryRequest;
import com.example.demo.service.progress.ClientProgressEntryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Trainers record measurement snapshots for their clients; clients read their own history via
 * {@code /me}. See AGENTS.md ("Upgrade: service layer decisions").
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/progress/entry")
public class ClientProgressEntryController {

    private final ClientProgressEntryService clientProgressEntryService;

    @RoleRequired({"MANAGER", "TRAINER"})
    @PostMapping
    public ResponseEntity<ClientProgressEntryDTO> create(@RequestBody CreateProgressEntryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(clientProgressEntryService.create(request));
    }

    @RoleRequired({"MANAGER", "TRAINER"})
    @GetMapping("/client/{clientId}")
    public ResponseEntity<List<ClientProgressEntryDTO>> getForClient(@PathVariable Integer clientId) {
        return ResponseEntity.ok(clientProgressEntryService.getForClient(clientId));
    }

    @RoleRequired("CLIENT")
    @GetMapping("/me")
    public ResponseEntity<List<ClientProgressEntryDTO>> getMine() {
        return ResponseEntity.ok(clientProgressEntryService.getMine());
    }
}
