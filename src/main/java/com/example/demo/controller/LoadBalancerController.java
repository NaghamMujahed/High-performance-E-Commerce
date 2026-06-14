package com.example.demo.controller;

import com.example.demo.model.Server;
import com.example.demo.service.LoadBalancerService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
public class LoadBalancerController {

    @Value("${APP_ROLE:worker}")
    private String role;

    @Value("${APP_NAME:unknown}")
    private String appName;

    @Autowired
    private LoadBalancerService loadBalancerService;

    @Autowired
    private RestTemplate restTemplate;

    /**
     * Normal Load Balanced Request
     */
    @GetMapping("/api/loadBalancer")
    public String handleRequest() throws InterruptedException {

        // إذا هذا السيرفر هو Load Balancer
        if (role.equals("loadbalancer")) {

            Server server = loadBalancerService.getBestServer();

            try {

                String url = server.getUrl() + "/api/loadBalancer";

                return restTemplate.getForObject(url, String.class);

            } finally {

                loadBalancerService.releaseServer(server);
            }
        }

        /*
         ينفذ داخل الـ Worker الحقيقي
         */

        Thread.sleep(10000);

        return """
        =========================
        Worker Response
        Server: %s
        Thread: %s
        =========================
        """.formatted(
                appName,
                Thread.currentThread().getName()
        );
    }
}
