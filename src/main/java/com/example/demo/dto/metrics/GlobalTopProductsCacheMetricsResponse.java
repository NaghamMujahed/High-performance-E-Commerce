package com.example.demo.dto.metrics;

import java.util.List;

public record GlobalTopProductsCacheMetricsResponse(
        String cache,
        TopProductsCacheMetricsResponse cluster,
        List<TopProductsCacheMetricsResponse> instances) {
}
