package com.example.demo.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    public void sendEmailSync(String customerName) {
        try {
            System.out.println("Started sending email to: " + customerName);
            Thread.sleep(3000);
            System.out.println("Email sent to: " + customerName);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }}
    @Async
    public void sendEmailAsync(String customerName) {
        try {
            System.out.println("Started sending email in background to: " + customerName);
            Thread.sleep(3000);
            System.out.println("Email sent to: " + customerName);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }}
}