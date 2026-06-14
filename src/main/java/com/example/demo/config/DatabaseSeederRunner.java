package com.example.demo.config;

import com.example.demo.service.DataSeedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class DatabaseSeederRunner implements ApplicationRunner {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseSeederRunner.class);

    private final DataSeedService dataSeedService;

    @Value("${app.seed.enabled:false}")
    private boolean enabled;
    @Value("${app.seed.reset:false}")
    private boolean reset;
    @Value("${app.seed.users:1000}")
    private int users;
    @Value("${app.seed.products:500}")
    private int products;
    @Value("${app.seed.orders:5000}")
    private int orders;
    @Value("${app.seed.sales:20000}")
    private int sales;
    @Value("${app.seed.owner-app:app1}")
    private String ownerApp;
    @Value("${APP_ROLE:worker}")
    private String role;
    @Value("${APP_NAME:unknown}")
    private String appName;

    public DatabaseSeederRunner(DataSeedService dataSeedService) {
        this.dataSeedService = dataSeedService;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled) {
            return;
        }
        if ("loadbalancer".equalsIgnoreCase(role)) {
            logger.info("[SEED] Skipping on load balancer instance.");
            return;
        }
        if (!"unknown".equalsIgnoreCase(appName) && !ownerApp.equalsIgnoreCase(appName)) {
            logger.info("[SEED] Skipping on {} because seed owner is {}.", appName, ownerApp);
            return;
        }

        logger.info("[SEED] Starting database seed on {}.", appName);
        dataSeedService.seed(users, products, orders, sales, reset);
    }
}
