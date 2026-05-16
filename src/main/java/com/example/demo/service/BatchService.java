package com.example.demo.service;

import com.example.demo.model.Sale;
import com.example.demo.repository.SaleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BatchService {

    private static final Logger logger =
            LoggerFactory.getLogger(BatchService.class);

    private final SaleRepository saleRepository;

    public BatchService(SaleRepository saleRepository) {
        this.saleRepository = saleRepository;
    }

    public void processAllAtOnce() {
        long totalRecords = saleRepository.countByProcessed(false);
        if (totalRecords == 0) {
            logger.info(" No unprocessed records found.");
            return;
        }
        var allSales = saleRepository.findAll();
        for (Sale sale : allSales) {
            sale.setProcessed(true);
            saleRepository.save(sale);
        }
        logger.info("[ALL-AT-ONCE] Processed: {} records", allSales.size());
    }


    @Scheduled(cron = "0 0 2 * * *")
    public void processSalesInChunks() {
        long totalRecords = saleRepository.countByProcessed(false);
        if (totalRecords == 0) {
            logger.info("[CHUNKS] No unprocessed records found.");
            return;
        }
        int chunkSize = (int) Math.max(50, Math.min(500, totalRecords / 10));

        int page = 0;
        Page<Sale> chunk;

        do {
            processOneBatch(page, chunkSize);
            page++;
            chunk = saleRepository.findByProcessed(
                    false,
                    PageRequest.of(page, chunkSize)
            );
        } while (chunk.hasNext());
    }

    @Transactional
    public void processOneBatch(int page, int size) {
        Page<Sale> chunk = saleRepository.findByProcessed(
                false,
                PageRequest.of(page, size)
        );
        logger.info("Found {} records", chunk.getNumberOfElements());

        for (Sale sale : chunk.getContent()) {
            sale.setProcessed(true);
            saleRepository.save(sale);
        }
    }
}