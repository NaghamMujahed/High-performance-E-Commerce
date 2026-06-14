package com.example.demo.controller;

import com.example.demo.model.Sale;
import com.example.demo.model.Server;
import com.example.demo.repository.SaleRepository;
import com.example.demo.service.BatchService;
import com.example.demo.service.LoadBalancerService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/batch")
public class BatchController {

    private final BatchService batchService;
    private final SaleRepository saleRepository;
    private final LoadBalancerService loadBalancerService;
    private final RestTemplate restTemplate;

    @Value("${APP_ROLE:worker}")
    private String role;

    public BatchController(
            BatchService batchService,
            SaleRepository saleRepository,
            LoadBalancerService loadBalancerService,
            RestTemplate restTemplate
    ) {
        this.batchService = batchService;
        this.saleRepository = saleRepository;
        this.loadBalancerService = loadBalancerService;
        this.restTemplate = restTemplate;
    }

    @PostMapping("/seed")
    public String seedData(@RequestParam int count) {

        if (role.equals("loadbalancer")) {

            Server server = loadBalancerService.getBatchServer();

            String url = server.getUrl() + "/batch/seed?count=" + count;

            return restTemplate.postForObject(url, null, String.class);
        }

        List<Sale> sales = new ArrayList<>();

        for (int i = 0; i < count; i++) {

            Sale sale = new Sale();

            sale.setProductName("Product " + i);
            sale.setQuantity(i + 1);
            sale.setTotalPrice((i + 1) * 10.0);
            sale.setSaleDate(LocalDate.now());
            sale.setProcessed(false);

            sales.add(sale);
        }

        saleRepository.saveAll(sales);

        return "Inserted " + count + " records";
    }

    @PostMapping("/process-all")
    public String processAll() {

        if (role.equals("loadbalancer")) {

            Server server = loadBalancerService.getBatchServer();

            String url = server.getUrl() + "/batch/process-all";

            return restTemplate.postForObject(url, null, String.class);
        }

        batchService.processAllAtOnce();

        return "Processed in one batch";
    }

    @PostMapping("/process-chunks")
    public String processChunks() {

        if (role.equals("loadbalancer")) {

            Server server = loadBalancerService.getBatchServer();

            String url = server.getUrl() + "/batch/process-chunks";

            return restTemplate.postForObject(url, null, String.class);
        }

        long pending = saleRepository.countByProcessed(false);

        if (pending == 0) {
            return "No unprocessed records found";
        }

        batchService.processSalesInChunks();

        return "Job launched for " + pending + " records";
    }

    @DeleteMapping("/delete-all")
    public String deleteAll() {

        if (role.equals("loadbalancer")) {

            Server server = loadBalancerService.getBatchServer();

            String url = server.getUrl() + "/batch/delete-all";

            restTemplate.delete(url);

            return "Deleted";
        }

        saleRepository.deleteAll();

        return "Deleted";
    }

    @GetMapping("/count")
    public Object count() {

        if (role.equals("loadbalancer")) {

            Server server = loadBalancerService.getBatchServer();

            String url = server.getUrl() + "/batch/count";

            return restTemplate.getForObject(url, Object.class);
        }

        return saleRepository.count();
    }
}
