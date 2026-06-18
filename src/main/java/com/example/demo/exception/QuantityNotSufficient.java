package com.example.demo.exception;

public class QuantityNotSufficient extends RuntimeException {

    public QuantityNotSufficient() {
        super("Quantity not sufficient");
    }
}