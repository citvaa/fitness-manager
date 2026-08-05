package com.example.demo.controller.progress;

import com.example.demo.annotation.RoleRequired;
import com.example.demo.dto.progress.ClientPersonalRecordDTO;
import com.example.demo.dto.progress.ClientProgressEntryDTO;
import com.example.demo.service.params.request.progress.UpsertPersonalRecordRequest;
import com.example.demo.service.params.request.progress.UpsertProgressEntryRequest;
import com.example.demo.service.params.response.ai.AiInsightResponse;
import com.example.demo.service.progress.ClientProgressService;
import com.example.demo.service.security.AuthenticatedUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/trainer/clients/{clientId}/progress")
public class TrainerClientProgressController {
    private final ClientProgressService service;
    private final AuthenticatedUserService authenticatedUser;

    private void authorize(Integer clientId) { service.assertTrainerCanAccess(authenticatedUser.trainer().getId(), clientId); }

    @RoleRequired("TRAINER") @GetMapping("/entries")
    public List<ClientProgressEntryDTO> entries(@PathVariable Integer clientId) { authorize(clientId); return service.entries(clientId); }
    @RoleRequired("TRAINER") @PostMapping("/entries")
    public ResponseEntity<ClientProgressEntryDTO> createEntry(@PathVariable Integer clientId, @RequestBody UpsertProgressEntryRequest request) { authorize(clientId); return ResponseEntity.status(HttpStatus.CREATED).body(service.createEntry(clientId, request)); }
    @RoleRequired("TRAINER") @PutMapping("/entries/{id}")
    public ClientProgressEntryDTO updateEntry(@PathVariable Integer clientId, @PathVariable Integer id, @RequestBody UpsertProgressEntryRequest request) { authorize(clientId); return service.updateEntry(clientId, id, request); }
    @RoleRequired("TRAINER") @DeleteMapping("/entries/{id}")
    public ResponseEntity<Void> deleteEntry(@PathVariable Integer clientId, @PathVariable Integer id) { authorize(clientId); service.deleteEntry(clientId, id); return ResponseEntity.noContent().build(); }

    @RoleRequired("TRAINER") @GetMapping("/records")
    public List<ClientPersonalRecordDTO> records(@PathVariable Integer clientId) { authorize(clientId); return service.records(clientId); }
    @RoleRequired("TRAINER") @PostMapping("/records")
    public ResponseEntity<ClientPersonalRecordDTO> createRecord(@PathVariable Integer clientId, @RequestBody UpsertPersonalRecordRequest request) { authorize(clientId); return ResponseEntity.status(HttpStatus.CREATED).body(service.createRecord(clientId, request)); }
    @RoleRequired("TRAINER") @PutMapping("/records/{id}")
    public ClientPersonalRecordDTO updateRecord(@PathVariable Integer clientId, @PathVariable Integer id, @RequestBody UpsertPersonalRecordRequest request) { authorize(clientId); return service.updateRecord(clientId, id, request); }
    @RoleRequired("TRAINER") @DeleteMapping("/records/{id}")
    public ResponseEntity<Void> deleteRecord(@PathVariable Integer clientId, @PathVariable Integer id) { authorize(clientId); service.deleteRecord(clientId, id); return ResponseEntity.noContent().build(); }
    @RoleRequired("TRAINER") @GetMapping("/summary")
    public AiInsightResponse summary(@PathVariable Integer clientId, @RequestParam(defaultValue = "false") boolean force) { authorize(clientId); return service.summary(clientId, force); }
}
