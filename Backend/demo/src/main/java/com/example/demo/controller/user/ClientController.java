package com.example.demo.controller.user;

import com.example.demo.annotation.RoleRequired;
import com.example.demo.dto.user.ClientDTO;
import com.example.demo.service.user.ClientService;
import com.example.demo.service.params.request.user.CreateUserRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/client")
public class ClientController {

    private final ClientService clientService;

    @RoleRequired("MANAGER")
    @GetMapping
    public List<ClientDTO> getAll() { return clientService.getAll(); }

    @RoleRequired("MANAGER")
    @GetMapping("/{id}")
    public ClientDTO getById(@PathVariable Integer id) { return clientService.getById(id); }

    @RoleRequired("MANAGER")
    @PostMapping
    public ResponseEntity<ClientDTO> create(@RequestBody CreateUserRequest request) {
        ClientDTO createdClient = clientService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdClient);
    }

    @RoleRequired("MANAGER")
    @PutMapping("/{id}")
    public ClientDTO update(@PathVariable Integer id, @RequestBody CreateUserRequest request) { return clientService.update(id, request); }

    @RoleRequired("MANAGER")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) { clientService.delete(id); return ResponseEntity.noContent().build(); }
}
