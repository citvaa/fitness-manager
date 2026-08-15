package com.example.demo.controller.dev;

import com.example.demo.annotation.RoleRequired;
import com.example.demo.config.dev.DemoDataSeeder;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dev")
@Profile("dev")
@RequiredArgsConstructor
public class DevDataController {
    private final DemoDataSeeder seeder;

    @PostMapping("/reseed")
    @RoleRequired("MANAGER")
    public ResponseEntity<Void> reseed() {
        seeder.reseed();
        return ResponseEntity.noContent().build();
    }
}
