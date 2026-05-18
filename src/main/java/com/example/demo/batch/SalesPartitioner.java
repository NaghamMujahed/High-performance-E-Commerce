package com.example.demo.batch;

import com.example.demo.repository.SaleRepository;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.HashMap;
import java.util.Map;

@Component
public class SalesPartitioner implements Partitioner {

    private static final Logger logger =
            LoggerFactory.getLogger(SalesPartitioner.class);
    private final SaleRepository saleRepository;

    public SalesPartitioner(SaleRepository saleRepository) {
        this.saleRepository = saleRepository;
    }

    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {
        Long minId = saleRepository.findMinUnprocessedId();
        Long maxId = saleRepository.findMaxUnprocessedId();

        if (minId == null || maxId == null) {
            logger.warn("No unprocessed records found");
            ExecutionContext context = new ExecutionContext();
            context.putLong("minId", 0L);
            context.putLong("maxId", -1L);
            Map<String, ExecutionContext> empty = new HashMap<>();
            empty.put("partition-0", context);
            return empty;
        }

        long totalRange = maxId - minId + 1;
        long range = (totalRange + gridSize - 1) / gridSize;

        Map<String, ExecutionContext> partitions = new HashMap<>();

        for (int i = 0; i < gridSize; i++) {
            long start = minId + (range * i);
            long end   = Math.min(start + range - 1, maxId);
            if (start > maxId) break;

            ExecutionContext context = new ExecutionContext();
            context.putLong("minId", start);
            context.putLong("maxId", end);
            partitions.put("partition-" + i, context);

            logger.info("Partition-{}: {} → {}", i, start, end);
        }
        return partitions;
    }
}