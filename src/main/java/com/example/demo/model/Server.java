package com.example.demo.model;

import java.util.concurrent.atomic.AtomicInteger;

public class Server {

    private String host;
    private int port;
    private int weight;

    private AtomicInteger activeConnections = new AtomicInteger(0);

    public Server(String host, int port, int weight) {
        this.host = host;
        this.port = port;
        this.weight = weight;
    }

    public String getUrl() {
        return "http://" + host + ":" + port;
    }

    public int getWeight() {
        return weight;
    }

    public int getActiveConnections() {
        return activeConnections.get();
    }

    public void incrementConnections() {
        activeConnections.incrementAndGet();
    }

    public void decrementConnections() {
        activeConnections.decrementAndGet();
    }

    public double getScore() {

        return (double) activeConnections.get() / weight;
    }

    @Override
    public String toString() {
        return "ServerNode{" +
                "url='" + getUrl() + '\'' +
                ", weight=" + weight +
                ", activeConnections=" + activeConnections +
                '}';
    }
}