package com.example.demo.service;

import com.example.demo.model.Server;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class LoadBalancerService {

    private static final Logger logger =
            LoggerFactory.getLogger(LoadBalancerService.class);

    @Value("${backend.instances}")
    private String backendInstances;

    private final List<Server> servers = new ArrayList<>();

    @PostConstruct
    public void init() {

        String[] instances = backendInstances.split(",");

        for (String instance : instances) {

            String[] parts = instance.split(":");

            String host = parts[0];
            int port = Integer.parseInt(parts[1]);
            int weight = Integer.parseInt(parts[2]);

            servers.add(new Server(host, port, weight));
        }

        logger.info("========== SERVERS LOADED ==========");

        for (Server server : servers) {

            logger.info(
                    "Server: {} | Weight: {}",
                    server.getUrl(),
                    server.getWeight()
            );
        }
    }


    public Server getBestServer() {
        if (servers.isEmpty()) {
            throw new IllegalStateException("No backend servers configured");
        }

        Server bestServer = servers.stream()
                .min((left, right) -> {
                    int scoreComparison = Double.compare(left.getScore(), right.getScore());
                    if (scoreComparison != 0) {
                        return scoreComparison;
                    }
                    return Integer.compare(right.getWeight(), left.getWeight());
                })
                .orElseThrow();

        bestServer.incrementConnections();
        logger.debug("Selected Server -> {} | Active Connections: {}",
                bestServer.getUrl(),
                bestServer.getActiveConnections());

        return bestServer;
    }


    public void releaseServer(Server server) {

        server.decrementConnections();

        logger.debug(
                "Released Server -> {} | Active Connections: {}",
                server.getUrl(),
                server.getActiveConnections()
        );
    }


    public Server getBatchServer() {

        return servers.stream()
                .filter(server -> server.getUrl().contains("app1"))
                .findFirst()
                .orElseThrow();
    }
}
