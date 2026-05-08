package com.example.demo.service;

import com.example.demo.model.Sale;
import com.example.demo.repository.SaleRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BatchService {

    private final SaleRepository saleRepository;

    public BatchService(SaleRepository saleRepository) {
        this.saleRepository = saleRepository;
    }

    public void processAllAtOnce() {
        System.out.println(" Processing all sales at once");
        var allSales = saleRepository.findAll();
        for (Sale sale : allSales) {
            sale.setProcessed(true);
            saleRepository.save(sale);
        }
        System.out.println(" Processing completed: " + allSales.size() + " records");
    }

    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void processSalesInChunks() {
        long totalRecords = saleRepository.countByProcessed(false);
        int dynamicChunkSize =
                (int) Math.max(50, Math.min(500, totalRecords / 10));
        System.out.println(" Dynamic Chunk Size: " + dynamicChunkSize);

        int pageNumber = 0;
        int totalProcessed = 0;
        Page<Sale> chunk;
        do {
            chunk = saleRepository.findByProcessed(
                    false,
                    PageRequest.of(pageNumber, dynamicChunkSize)
            );
            for (Sale sale : chunk.getContent()) {
                sale.setProcessed(true);
                saleRepository.save(sale);
                totalProcessed++;
            }
            System.out.println(" Batch " + pageNumber + ": " + totalProcessed + " records");
            pageNumber++;
        } while (chunk.hasNext());

        System.out.println(" Processing completed: " + totalProcessed + " records");
    }
}