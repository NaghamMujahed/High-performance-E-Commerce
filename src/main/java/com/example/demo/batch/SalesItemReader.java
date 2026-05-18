package com.example.demo.batch;

import com.example.demo.model.Sale;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.database.JpaCursorItemReader;
import java.util.Map;

@Component
@StepScope
public class SalesItemReader extends JpaCursorItemReader<Sale> {
    private static final Logger logger = LoggerFactory.getLogger(SalesItemReader.class);
    public SalesItemReader(EntityManagerFactory entityManagerFactory,
            @Value("#{stepExecutionContext['minId']}") Long minId,
            @Value("#{stepExecutionContext['maxId']}") Long maxId) {

        logger.info("[READER] minId={} maxId={} | Thread: {}", minId, maxId, Thread.currentThread().getName());

        this.setName("salesItemReader");
        this.setEntityManagerFactory(entityManagerFactory);
        this.setQueryString("""
                SELECT s FROM Sale s
                WHERE s.id BETWEEN :minId AND :maxId
                AND s.processed = false
                ORDER BY s.id
                """);
        this.setParameterValues(Map.of("minId", minId, "maxId", maxId));
        this.setSaveState(true);
    }
}