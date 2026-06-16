package com.example.demo.controller.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.metrics.TimerMetricsResponse;
import com.example.demo.dto.metrics.TopProductsCacheMetricsResponse;

import java.util.concurrent.TimeUnit;

@RestController
public class LocalCacheMetricsController {

    private final MeterRegistry meterRegistry;

    @Value("${INSTANCE_ID:unknown}")
    private String instanceId;

    public LocalCacheMetricsController(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @GetMapping("/internal/metrics/top-products-cache")
    public TopProductsCacheMetricsResponse topProductsCacheMetrics() {

        double hit = counterValue("app.cache.hit", "cache", "topProducts");
        double miss = counterValue("app.cache.miss", "cache", "topProducts");

        double total = hit + miss;
        double hitRatio = total == 0 ? 0 : (hit / total) * 100;

        return new TopProductsCacheMetricsResponse(
                instanceId,
                true,

                hit,
                miss,
                hitRatio,

                counterValue("app.cache.stale_served", "cache", "topProducts"),
                counterValue("app.cache.fallback_db", "cache", "topProducts"),
                counterValue("app.cache.rebuild.success", "cache", "topProducts"),

                counterValue("app.distributed_lock.acquired", "lock", "topProductsRebuild"),
                counterValue("app.distributed_lock.skipped", "lock", "topProductsRebuild"),

                counterValue("app.db.query", "query", "findTopSellingProducts"),

                timerSummary("app.cache.rebuild.duration", "cache", "topProducts"),
                timerSummary("app.distributed_lock.wait", "lock", "topProductsRebuild"));
    }

    private double counterValue(String metricName, String tagKey, String tagValue) {
        Counter counter = meterRegistry.find(metricName)
                .tag(tagKey, tagValue)
                .counter();

        return counter == null ? 0 : counter.count();
    }

    private TimerMetricsResponse timerSummary(String metricName, String tagKey, String tagValue) {
        Timer timer = meterRegistry.find(metricName)
                .tag(tagKey, tagValue)
                .timer();

        if (timer == null) {
            return new TimerMetricsResponse(0, 0, 0, 0);
        }

        return new TimerMetricsResponse(
                timer.count(),
                timer.totalTime(TimeUnit.MILLISECONDS),
                timer.mean(TimeUnit.MILLISECONDS),
                timer.max(TimeUnit.MILLISECONDS));
    }
}