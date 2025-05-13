package com.example.demo.controller;

import com.example.demo.dto.UserDTO;
import com.example.demo.service.impl.UserServiceImpl;
import com.example.demo.service.params.request.UserRequest;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/user")
public class UserController {

    private final UserServiceImpl userServiceImpl;

    public UserController(UserServiceImpl userServiceImpl) {
        this.userServiceImpl = userServiceImpl;
    }

    @Operation(summary = "Get all users")
    @GetMapping
    public List<UserDTO> getAllUsers() {
        return userServiceImpl.getAllUsers();
    }

    @Operation(summary = "Get user by ID")
    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> getUserById(@PathVariable Integer id) {
        return userServiceImpl.getUserById(id)
                .map(ResponseEntity::ok)
                .orElseGet(()-> ResponseEntity.notFound().build());
    }

    @Operation(summary = "Create new user")
    @PostMapping
    public UserDTO createUser(@RequestBody UserRequest request) {
        return userServiceImpl.createUser(request);
    }

    @Operation(summary = "Update user")
    @PutMapping("/{id}")
    public ResponseEntity<UserDTO> updateUser(@PathVariable Integer id, @RequestBody UserRequest request) {
        return ResponseEntity.ok(userServiceImpl.updateUser(id, request));
    }

    @Operation(summary = "Delete user")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Integer id) {
        userServiceImpl.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
