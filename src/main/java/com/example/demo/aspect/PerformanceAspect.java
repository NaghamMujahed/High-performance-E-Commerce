package com.example.demo.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class PerformanceAspect {

    private static final Logger logger = LoggerFactory.getLogger(PerformanceAspect.class);

    @Around("execution(* com.example.demo..service.*.*(..))")
    public Object measurePerformance(ProceedingJoinPoint joinPoint) throws Throwable {

        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();

        logger.info(" Started: {}.{}", className, methodName);

        long start = System.currentTimeMillis();

        try {
            Object result = joinPoint.proceed();
            long time = System.currentTimeMillis() - start;
            logger.info("Finished: {}.{} | Time: {}ms", className, methodName, time);
            return result;

        } catch (Exception e) {
            long time = System.currentTimeMillis() - start;
            logger.error(
                    "Failed: {}.{} | Time: {}ms | Reason: {}",
                    className,
                    methodName,
                    time,
                    e.getMessage());
            throw e;
        }
    }

    @Around("execution(* com.example.demo.service.ProductService.purchase*(..))")
    public Object measurePurchase(ProceedingJoinPoint joinPoint) throws Throwable {

        String methodName = joinPoint.getSignature().getName();

        logger.info("[PURCHASE] Started: {} | Thread: {}", methodName, Thread.currentThread().getName());
        long start = System.currentTimeMillis();

        Object result = joinPoint.proceed();

        long time = System.currentTimeMillis() - start;

        logger.info(
                "[PURCHASE] Finished: {} | Time: {}ms | Thread: {}",
                methodName,
                time,
                Thread.currentThread().getName());

        return result;
    }

    @Around("execution(* com.example.demo.service.ProductService.safeGetTopProductDetails*(..)) || " +
            "execution(* com.example.demo.service.ProductService.loadTopProductsFromDb.*(..)) || " +
            "execution(* com.example.demo.service.ProductService.updateProduct.*(..))")
    public Object measureCacheRelatedMethods(ProceedingJoinPoint pjp) throws Throwable {
        long start = System.nanoTime();

        try {
            return pjp.proceed();
        } finally {
            long durationMs = (System.nanoTime() - start) / 1_000_000;

            logger.info("CACHE_PERF method={} durationMs={}",
                    pjp.getSignature().toShortString(),
                    durationMs);
        }
    }
}