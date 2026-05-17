package com.example.demo.controller;

import com.example.demo.model.Server;
import com.example.demo.service.LoadBalancerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
public class LoadBalancerController {

    @Value("${APP_ROLE}")
    private String role;

    @Value("${APP_NAME:unknown}")
    private String appName;

    @Autowired
    private LoadBalancerService loadBalancerService;

    @Autowired
    private RestTemplate restTemplate;

    @GetMapping("/api/loadBalancer")
    public String handleRequest() throws InterruptedException {
        if (role.equals("loadbalancer")) {

            Server server = loadBalancerService.getBestServer();

            try {

                String url = server.getUrl() + "/api/loadBalancer";

                return restTemplate.getForObject(url, String.class);

            } finally {

                loadBalancerService.releaseServer(server);
            }
        }

        Thread.sleep(2000);

        return "Response from: " + appName;
    }
}