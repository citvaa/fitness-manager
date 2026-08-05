package com.example.demo.controller.progress;

import com.example.demo.annotation.RoleRequired;
import com.example.demo.dto.progress.ClientProgressInsightDTO;
import com.example.demo.service.progress.ClientProgressInsightService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/progress/insight")
public class ClientProgressInsightController {

    private final ClientProgressInsightService clientProgressInsightService;

    @RoleRequired({"MANAGER", "TRAINER"})
    @GetMapping("/client/{clientId}")
    public ResponseEntity<ClientProgressInsightDTO> getSummary(@PathVariable Integer clientId) {
        return ResponseEntity.ok(clientProgressInsightService.getSummary(clientId));
    }

    @RoleRequired("CLIENT")
    @GetMapping("/me")
    public ResponseEntity<ClientProgressInsightDTO> getMySummary() {
        return ResponseEntity.ok(clientProgressInsightService.getMySummary());
    }
}
