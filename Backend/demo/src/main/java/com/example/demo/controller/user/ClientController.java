package com.example.demo.controller.user;

import com.example.demo.annotation.RoleRequired;
import com.example.demo.dto.user.ClientDTO;
import com.example.demo.service.user.ClientService;
import com.example.demo.service.params.request.user.CreateUserRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/client")
public class ClientController {

    private final ClientService clientService;

    @RoleRequired("MANAGER")
    @PostMapping
    public ResponseEntity<ClientDTO> create(@RequestBody CreateUserRequest request) {
        ClientDTO createdClient = clientService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdClient);
    }

    @RoleRequired("MANAGER")
    @GetMapping
    public ResponseEntity<List<ClientDTO>> getAll() {
        return ResponseEntity.ok(clientService.getAll());
    }

    /** Lets the frontend learn its own client id, e.g. for subscribing to /topic/client{id}. */
    @RoleRequired("CLIENT")
    @GetMapping("/me")
    public ResponseEntity<ClientDTO> getMe() {
        return ResponseEntity.ok(clientService.getMe());
    }
}
