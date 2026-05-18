package com.example.demo.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
public class WithoutLoadBalancerController {

    private static final Logger logger =
            LoggerFactory.getLogger(WithoutLoadBalancerController.class);

    @Value("${APP_NAME:unknown}")
    private String appName;

    @GetMapping("/api/without-load-balancer")
    public String withoutLoadBalancer() throws InterruptedException {

        logger.info("Request handled ONLY by {}", appName);

        Thread.sleep(2000);

        return "Handled ONLY by: " + appName;
    }
}


