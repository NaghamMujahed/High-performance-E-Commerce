package com.example.demo.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WorkerController {

    @Value("${APP_NAME:unknown}")
    private String appName;

    @GetMapping("/worker/test")
    public String test() throws InterruptedException {

        Thread.sleep(2000);

        return "Response from: " + appName;
    }
}