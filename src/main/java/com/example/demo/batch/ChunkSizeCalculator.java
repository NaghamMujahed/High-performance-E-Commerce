package com.example.demo.batch;

import org.springframework.stereotype.Component;

@Component
public class ChunkSizeCalculator {

    public int calculate(long totalRecords) {
        if (totalRecords <= 100) return 10;
        if (totalRecords <= 1_000) return 100;
        if (totalRecords <= 10_000) return 500;
        return 1000;
    }
}