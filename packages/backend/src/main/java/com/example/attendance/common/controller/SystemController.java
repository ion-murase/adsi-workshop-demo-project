package com.example.attendance.common.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/system")
public class SystemController {

    public record HealthResponse(String status, String application, Instant timestamp) {
    }

    @GetMapping("/health")
    public HealthResponse health() {
        return new HealthResponse("ok", "attendance", Instant.now());
    }
}
