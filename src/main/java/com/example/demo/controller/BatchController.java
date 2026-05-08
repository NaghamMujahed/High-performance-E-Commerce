package com.example.demo.controller;

import com.example.demo.service.BatchService;
import com.example.demo.model.Sale;
import com.example.demo.repository.SaleRepository;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/batch")
public class BatchController {

    private final BatchService batchService;
    private final SaleRepository saleRepository;

    public BatchController(BatchService batchService,
                           SaleRepository saleRepository) {
        this.batchService = batchService;
        this.saleRepository = saleRepository;
    }

    @PostMapping("/seed")
    public void seedData(@RequestParam int count) {
        for (int i = 0; i < count; i++) {
            Sale sale = new Sale();
            sale.setProductName("Product " + i);
            sale.setQuantity(i + 1);
            sale.setTotalPrice((i + 1) * 10.0);
            sale.setSaleDate(LocalDate.now());
            sale.setProcessed(false);
            saleRepository.save(sale);
        }
    }

    @PostMapping("/process-all")
    public String processAll() {
        batchService.processAllAtOnce();
        return "Processed in one batch";
    }

    @PostMapping("/process-chunks")
    public String processChunks() {
        batchService.processSalesInChunks();
        return "Processing was done in batches";
    }
}