package com.example.demo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "app.cluster")
public class ClusterProperties {

    private List<String> instanceUrls = new ArrayList<>();

    public List<String> getInstanceUrls() {
        return instanceUrls;
    }

    public void setInstanceUrls(List<String> instanceUrls) {
        this.instanceUrls = instanceUrls;
    }
}
