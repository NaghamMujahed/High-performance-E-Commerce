package com.example.demo.controller;

import com.example.demo.model.Server;
import com.example.demo.service.LoadBalancerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/api")
public class LoadBalancerController {

    @Autowired
    private LoadBalancerService loadBalancerService;

    @Autowired
    private RestTemplate restTemplate;

    @GetMapping("/test")
    public String balanceRequest() {

        Server server = loadBalancerService.getBestServer();

        try {

            String url = server.getUrl() + "/worker/test";

            return restTemplate.getForObject(url, String.class);

        } finally {

            loadBalancerService.releaseServer(server);
        }
    }
}