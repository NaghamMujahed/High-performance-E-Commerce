package com.example.demo.batch;

import com.example.demo.model.Sale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Component
public class SalesItemProcessor implements ItemProcessor<Sale, Sale> {

    private static final Logger logger =
            LoggerFactory.getLogger(SalesItemProcessor.class);

    @Override
    public Sale process(Sale sale) {

        logger.info("[PROCESSOR] id={} processed={} | Thread: {}",
                sale.getId(), sale.isProcessed(), Thread.currentThread().getName());
        sale.setProcessed(true);
        return sale;
    }
}