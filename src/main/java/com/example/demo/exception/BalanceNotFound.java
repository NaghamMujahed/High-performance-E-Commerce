package com.example.demo.exception;

public class BalanceNotFound extends RuntimeException {

    public BalanceNotFound() {
        super("Balance not found");
    }
}