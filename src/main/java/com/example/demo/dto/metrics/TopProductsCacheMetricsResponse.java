package com.example.demo.dto.metrics;

public record TopProductsCacheMetricsResponse(
        String instance,
        boolean available,

        double cacheHit,
        double cacheMiss,
        double cacheHitRatio,

        double staleServed,
        double fallbackDb,
        double rebuildSuccess,

        double lockAcquired,
        double lockSkipped,

        double dbQueries,

        TimerMetricsResponse rebuildDuration,
        TimerMetricsResponse lockWait) {
}