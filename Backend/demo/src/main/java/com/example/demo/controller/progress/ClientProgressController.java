package com.example.demo.controller.progress;

import com.example.demo.annotation.RoleRequired;
import com.example.demo.dto.progress.ClientPersonalRecordDTO;
import com.example.demo.dto.progress.ClientProgressEntryDTO;
import com.example.demo.service.params.response.ai.AiInsightResponse;
import com.example.demo.service.progress.ClientProgressService;
import com.example.demo.service.security.AuthenticatedUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/client/progress")
public class ClientProgressController {
    private final ClientProgressService service;
    private final AuthenticatedUserService authenticatedUser;

    @RoleRequired("CLIENT") @GetMapping("/entries")
    public List<ClientProgressEntryDTO> entries() { return service.entries(authenticatedUser.client().getId()); }
    @RoleRequired("CLIENT") @GetMapping("/records")
    public List<ClientPersonalRecordDTO> records() { return service.records(authenticatedUser.client().getId()); }
    @RoleRequired("CLIENT") @GetMapping("/summary")
    public AiInsightResponse summary() { return service.summary(authenticatedUser.client().getId(), false); }
}
