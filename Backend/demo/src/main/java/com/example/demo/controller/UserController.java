package com.example.demo.controller;

import com.example.demo.dto.UserDTO;
import com.example.demo.service.UserService;
import com.example.demo.service.impl.UserServiceImpl;
import com.example.demo.service.params.request.CreateUserRequest;
import com.example.demo.service.params.request.RegisterUserRequest;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/user")
public class UserController {

    private final UserService userService;

    public UserController(UserServiceImpl userService) {
        this.userService = userService;
    }

    @Operation(summary = "Get all users")
    @GetMapping
    public List<UserDTO> getAll() {
        return userService.getAll();
    }

    @Operation(summary = "Get user by ID")
    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> getById(@PathVariable Integer id) {
        return userService.getById(id)
                .map(ResponseEntity::ok)
                .orElseGet(()-> ResponseEntity.notFound().build());
    }

    @Operation(summary = "Create new user")
    @PostMapping
    public UserDTO create(@RequestBody CreateUserRequest request) {
        return userService.create(request);
    }

    @Operation(summary = "Update user")
    @PutMapping("/{id}")
    public ResponseEntity<UserDTO> update(@PathVariable Integer id, @RequestBody CreateUserRequest request) {
        return ResponseEntity.ok(userService.update(id, request));
    }

    @Operation(summary = "Delete user")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Register user")
    @PostMapping("/register")
    public ResponseEntity<Void> register(@RequestBody RegisterUserRequest request) {
        userService.register(request);
        return ResponseEntity.ok().build();
    }
}
