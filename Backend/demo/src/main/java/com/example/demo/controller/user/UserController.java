package com.example.demo.controller.user;

import com.example.demo.annotation.RoleRequired;
import com.example.demo.dto.user.UserDTO;
import com.example.demo.enums.NotificationPreference;
import com.example.demo.enums.Role;
import com.example.demo.service.user.UserService;
import com.example.demo.service.params.request.user.*;
import com.example.demo.service.params.response.user.LoginResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;

    @RoleRequired("MANAGER")
    @GetMapping
    public Page<UserDTO> getUsers(SearchUserRequest request) {
        return userService.getUsers(request);
    }

    @RoleRequired("MANAGER")
    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> getById(@PathVariable Integer id) {
        return userService.getById(id)
                .map(ResponseEntity::ok)
                .orElseGet(()-> ResponseEntity.notFound().build());
    }

    @RoleRequired("MANAGER")
    @PostMapping
    public ResponseEntity<UserDTO> create(@RequestBody CreateUserRequest request) {
        UserDTO createdUser = userService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
    }

    @RoleRequired("MANAGER")
    @PutMapping("/{id}")
    public ResponseEntity<UserDTO> update(@PathVariable Integer id, @RequestBody CreateUserRequest request) {
        return ResponseEntity.ok(userService.update(id, request));
    }

    @RoleRequired("MANAGER")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/register")
    public ResponseEntity<Void> register(@RequestBody RegisterUserRequest request) {
        userService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginUserRequest request) {
        return ResponseEntity.ok(userService.login(request));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Void> requestPasswordReset(@RequestParam String email) {
        userService.requestPasswordReset(email);
        return ResponseEntity.ok().build();
    }

    @PostMapping("reset-password")
    public ResponseEntity<Void> resetPassword(@RequestBody ResetPasswordRequest request) {
        userService.resetPassword(request);
        return ResponseEntity.ok().build();
    }

    @RoleRequired("MANAGER")
    @PostMapping("/{id}/role")
    public ResponseEntity<Void> addRole(@PathVariable Integer id, @RequestParam Role role) {
        userService.addRole(id, role);
        return ResponseEntity.ok().build();
    }

    @RoleRequired("MANAGER")
    @DeleteMapping("/{id}/role")
    public ResponseEntity<Void> removeRole(@PathVariable Integer id, @RequestParam Role role) {
        userService.removeRole(id, role);
        return ResponseEntity.ok().build();
    }

    @RoleRequired("MANAGER")
    @PatchMapping("/{id}/notification-preference")
    public ResponseEntity<Void> updateNotificationPreference(@PathVariable Integer id, NotificationPreference notificationPreference) {
        userService.updateNotificationPreference(id, notificationPreference);
        return ResponseEntity.ok().build();
    }
}
