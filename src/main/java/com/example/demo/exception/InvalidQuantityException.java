package com.example.demo.exception;

public class InvalidQuantityException extends RuntimeException {

    public InvalidQuantityException(int quantity) {
        super("Invalid Quantity " + quantity);
    }
}