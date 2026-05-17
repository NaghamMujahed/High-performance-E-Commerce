package com.example.demo.service;

import com.example.demo.model.Server;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
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


    public synchronized Server getBestServer() {

        logger.info("========== BEFORE SELECTION ==========");

        for (Server server : servers) {

            logger.info(
                    "Server: {} | Active: {} | Weight: {} | Score: {}",
                    server.getUrl(),
                    server.getActiveConnections(),
                    server.getWeight(),
                    server.getScore()
            );
        }

        double minScore = servers.stream()
                .mapToDouble(Server::getScore)
                .min()
                .orElse(0);

        List<Server> candidates = servers.stream()
                .filter(server -> server.getScore() == minScore)
                .toList();


        Server bestServer = candidates.stream()
                .max(Comparator.comparingInt(Server::getWeight))
                .orElseThrow();

        logger.info(
                "Selected Server -> {}",
                bestServer.getUrl()
        );

        bestServer.incrementConnections();

        logger.info("========== AFTER SELECTION ==========");

        for (Server server : servers) {

            logger.info(
                    "Server: {} | Active: {} | Weight: {} | Score: {}",
                    server.getUrl(),
                    server.getActiveConnections(),
                    server.getWeight(),
                    server.getScore()
            );
        }

        return bestServer;
    }


    public void releaseServer(Server server) {

        server.decrementConnections();

        logger.info(
                "Released Server -> {} | Active Connections: {}",
                server.getUrl(),
                server.getActiveConnections()
        );
    }
}