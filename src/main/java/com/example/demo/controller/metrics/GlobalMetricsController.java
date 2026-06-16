package com.example.demo.controller.metrics;

import com.example.demo.config.ClusterProperties;
import com.example.demo.dto.metrics.GlobalTopProductsCacheMetricsResponse;
import com.example.demo.dto.metrics.TimerMetricsResponse;
import com.example.demo.dto.metrics.TopProductsCacheMetricsResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@RestController
public class GlobalMetricsController {

    private final RestTemplate restTemplate;
    private final ClusterProperties clusterProperties;

    @Value("${app.role:worker}")
    private String role;

    public GlobalMetricsController(RestTemplate restTemplate,
            ClusterProperties clusterProperties) {
        this.restTemplate = restTemplate;
        this.clusterProperties = clusterProperties;
    }

    @GetMapping("/metrics/global/top-products-cache")
    public GlobalTopProductsCacheMetricsResponse globalTopProductsCacheMetrics() {

        if (!"loadbalancer".equals(role)) {
            throw new IllegalStateException(
                    "This endpoint should be called from the loadbalancer instance only");
        }

        List<TopProductsCacheMetricsResponse> instances = new ArrayList<>();

        for (String instanceUrl : clusterProperties.getInstanceUrls()) {
            instances.add(fetchInstanceMetrics(instanceUrl));
        }

        TopProductsCacheMetricsResponse clusterTotal = aggregate(instances);

        return new GlobalTopProductsCacheMetricsResponse(
                "topProducts",
                clusterTotal,
                instances);
    }

    private TopProductsCacheMetricsResponse fetchInstanceMetrics(String instanceUrl) {
        try {
            String url = instanceUrl + "/internal/metrics/top-products-cache";

            return restTemplate.getForObject(
                    url,
                    TopProductsCacheMetricsResponse.class);

        } catch (Exception ex) {
            return new TopProductsCacheMetricsResponse(
                    instanceUrl,
                    false,

                    0,
                    0,
                    0,

                    0,
                    0,
                    0,

                    0,
                    0,

                    0,

                    new TimerMetricsResponse(0, 0, 0, 0),
                    new TimerMetricsResponse(0, 0, 0, 0));
        }
    }

    private TopProductsCacheMetricsResponse aggregate(
            List<TopProductsCacheMetricsResponse> instances) {
        double hit = 0;
        double miss = 0;
        double staleServed = 0;
        double fallbackDb = 0;
        double rebuildSuccess = 0;
        double lockAcquired = 0;
        double lockSkipped = 0;
        double dbQueries = 0;

        long rebuildCount = 0;
        double rebuildTotalMs = 0;
        double rebuildMaxMs = 0;

        long lockWaitCount = 0;
        double lockWaitTotalMs = 0;
        double lockWaitMaxMs = 0;

        for (TopProductsCacheMetricsResponse item : instances) {
            if (!item.available()) {
                continue;
            }

            hit += item.cacheHit();
            miss += item.cacheMiss();
            staleServed += item.staleServed();
            fallbackDb += item.fallbackDb();
            rebuildSuccess += item.rebuildSuccess();
            lockAcquired += item.lockAcquired();
            lockSkipped += item.lockSkipped();
            dbQueries += item.dbQueries();

            rebuildCount += item.rebuildDuration().count();
            rebuildTotalMs += item.rebuildDuration().totalTimeMs();
            rebuildMaxMs = Math.max(rebuildMaxMs, item.rebuildDuration().maxMs());

            lockWaitCount += item.lockWait().count();
            lockWaitTotalMs += item.lockWait().totalTimeMs();
            lockWaitMaxMs = Math.max(lockWaitMaxMs, item.lockWait().maxMs());
        }

        double totalCacheAccess = hit + miss;
        double hitRatio = totalCacheAccess == 0 ? 0 : (hit / totalCacheAccess) * 100;

        TimerMetricsResponse rebuildDuration = new TimerMetricsResponse(
                rebuildCount,
                rebuildTotalMs,
                rebuildCount == 0 ? 0 : rebuildTotalMs / rebuildCount,
                rebuildMaxMs);

        TimerMetricsResponse lockWait = new TimerMetricsResponse(
                lockWaitCount,
                lockWaitTotalMs,
                lockWaitCount == 0 ? 0 : lockWaitTotalMs / lockWaitCount,
                lockWaitMaxMs);

        return new TopProductsCacheMetricsResponse(
                "cluster-total",
                true,

                hit,
                miss,
                hitRatio,

                staleServed,
                fallbackDb,
                rebuildSuccess,

                lockAcquired,
                lockSkipped,

                dbQueries,

                rebuildDuration,
                lockWait);
    }
}