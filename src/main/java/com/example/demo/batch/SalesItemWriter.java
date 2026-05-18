package com.example.demo.batch;

import com.example.demo.model.Sale;
import com.example.demo.repository.SaleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

@Component
public class SalesItemWriter implements ItemWriter<Sale> {

    private static final Logger logger =
            LoggerFactory.getLogger(SalesItemWriter.class);
    private final SaleRepository saleRepository;

    public SalesItemWriter(SaleRepository saleRepository) {
        this.saleRepository = saleRepository;
    }

    @Override
    public void write(Chunk<? extends Sale> chunk) {

        saleRepository.saveAll(chunk.getItems());

        logger.info("[WRITER] Saved {} records | Thread: {}",
                chunk.getItems().size(),
                Thread.currentThread().getName());
    }
}