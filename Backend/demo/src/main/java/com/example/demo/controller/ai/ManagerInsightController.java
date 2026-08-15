package com.example.demo.controller.ai;

import com.example.demo.annotation.RoleRequired;
import com.example.demo.service.ai.ManagerInsightService;
import com.example.demo.service.params.response.ai.ManagerInsightResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/manager/insights")
public class ManagerInsightController {
    private final ManagerInsightService service;

    @RoleRequired("MANAGER")
    @GetMapping
    public ManagerInsightResponse get(@RequestParam(defaultValue = "false") boolean force) { return service.getInsights(force); }
}
