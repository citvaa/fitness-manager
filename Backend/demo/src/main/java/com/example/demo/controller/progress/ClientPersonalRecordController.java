package com.example.demo.controller.progress;

import com.example.demo.annotation.RoleRequired;
import com.example.demo.dto.progress.ClientPersonalRecordDTO;
import com.example.demo.service.params.request.progress.CreatePersonalRecordRequest;
import com.example.demo.service.progress.ClientPersonalRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/progress/record")
public class ClientPersonalRecordController {

    private final ClientPersonalRecordService clientPersonalRecordService;

    @RoleRequired({"MANAGER", "TRAINER"})
    @PostMapping
    public ResponseEntity<ClientPersonalRecordDTO> create(@RequestBody CreatePersonalRecordRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(clientPersonalRecordService.create(request));
    }

    @RoleRequired({"MANAGER", "TRAINER"})
    @GetMapping("/client/{clientId}")
    public ResponseEntity<List<ClientPersonalRecordDTO>> getForClient(@PathVariable Integer clientId) {
        return ResponseEntity.ok(clientPersonalRecordService.getForClient(clientId));
    }

    @RoleRequired("CLIENT")
    @GetMapping("/me")
    public ResponseEntity<List<ClientPersonalRecordDTO>> getMine() {
        return ResponseEntity.ok(clientPersonalRecordService.getMine());
    }
}
