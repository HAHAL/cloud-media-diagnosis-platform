package com.example.clouddiagnosis.controller;

import com.example.clouddiagnosis.model.response.HealthResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    @GetMapping("/api/health")
    public HealthResponse health() {
        return new HealthResponse("UP", "web-diagnosis-platform");
    }
}
