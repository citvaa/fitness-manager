package com.example.demo.controller.dev;

import com.example.demo.annotation.RoleRequired;
import com.example.demo.config.dev.DevDataSeeder;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Dev-only endpoint for wiping and rebuilding all dev/test data without a Docker container
 * restart or volume wipe - see AGENTS.md "Upgrade: dev-data ownership decisions".
 *
 * <p>Restricted two ways: {@code @Profile("dev")} means this controller (and its
 * {@code /api/dev/**} routes) don't even exist as beans outside the dev profile, and
 * {@code @RoleRequired("MANAGER")} adds a real auth check on top of that - this is a genuinely
 * destructive, whole-table-wiping operation, so "only reachable on dev" alone felt too thin a
 * guard given how cheap the extra check is. A manager JWT is required exactly like any other
 * admin-facing endpoint in this app.
 */
@Profile("dev")
@RestController
@RequestMapping("/api/dev")
@RequiredArgsConstructor
public class DevDataController {

    private final DevDataSeeder devDataSeeder;

    @RoleRequired("MANAGER")
    @PostMapping("/reseed")
    public ResponseEntity<String> reseed() {
        devDataSeeder.reseed();
        return ResponseEntity.ok("Dev podaci su uspešno ponovo zasejani.");
    }
}
