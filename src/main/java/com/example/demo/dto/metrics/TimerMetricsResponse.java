package com.example.demo.dto.metrics;

public record TimerMetricsResponse(
        long count,
        double totalTimeMs,
        double meanMs,
        double maxMs) {
}
