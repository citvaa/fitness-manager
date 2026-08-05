package com.example.demo.controller.insights;

import com.example.demo.annotation.RoleRequired;
import com.example.demo.dto.insights.ManagerInsightsDTO;
import com.example.demo.service.insights.ManagerInsightsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/insights/manager")
public class ManagerInsightsController {

    private final ManagerInsightsService managerInsightsService;

    @RoleRequired("MANAGER")
    @GetMapping
    public ResponseEntity<ManagerInsightsDTO> getInsights() {
        return ResponseEntity.ok(managerInsightsService.getInsights());
    }

    @RoleRequired("MANAGER")
    @PostMapping("/refresh")
    public ResponseEntity<ManagerInsightsDTO> refreshInsights() {
        return ResponseEntity.ok(managerInsightsService.refreshInsights());
    }
}
