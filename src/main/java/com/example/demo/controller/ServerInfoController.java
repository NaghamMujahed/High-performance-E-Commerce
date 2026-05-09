package com.example.demo.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/server")
public class ServerInfoController {

    @GetMapping("/info")
    public Map<String, String> getServerInfo(HttpSession session) {
        String hostname = System.getenv("HOSTNAME") != null ?
                System.getenv("HOSTNAME") : "local-machine";
        String sessionId = session.getId();

        System.out.println(" Request Received:");
        System.out.println("    Hostname: " + hostname);
        System.out.println("    Session ID: " + sessionId);
        System.out.println("    Thread: " + Thread.currentThread().getName());


        Map<String, String> info = new HashMap<>();
        info.put("status", "healthy");
        info.put("hostname", hostname);
        info.put("port", System.getenv("SERVER_PORT") != null ?
                System.getenv("SERVER_PORT") : "8080");
        info.put("sessionId", sessionId);

        return info;
    }
}