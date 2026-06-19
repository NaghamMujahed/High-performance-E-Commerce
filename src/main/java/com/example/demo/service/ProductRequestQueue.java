package com.example.demo.service;

import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import org.springframework.stereotype.Service;

@Service
public class ProductRequestQueue {

    private static final int STRIPE_COUNT = 64;

    private final ReentrantLock[] locks = IntStream.range(0, STRIPE_COUNT)
            .mapToObj(index -> new ReentrantLock(true))
            .toArray(ReentrantLock[]::new);

    public <T> T executeForProduct(Long productId, Supplier<T> action) {
        ReentrantLock lock = locks[Math.floorMod(productId.hashCode(), locks.length)];
        lock.lock();
        try {
            return action.get();
        } finally {
            lock.unlock();
        }
    }
}
