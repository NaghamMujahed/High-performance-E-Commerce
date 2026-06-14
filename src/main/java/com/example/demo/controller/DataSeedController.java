package com.example.demo.controller;

import com.example.demo.service.DataSeedService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/seed")
public class DataSeedController {
    private final DataSeedService dataSeedService;

    public DataSeedController(DataSeedService dataSeedService) {
        this.dataSeedService = dataSeedService;
    }

    @PostMapping("/ecommerce")
    public DataSeedService.SeedResult seedEcommerceData(
            @RequestParam(defaultValue = "1000") int users,
            @RequestParam(defaultValue = "500") int products,
            @RequestParam(defaultValue = "5000") int orders,
            @RequestParam(defaultValue = "20000") int sales,
            @RequestParam(defaultValue = "false") boolean reset
    ) {
        return dataSeedService.seed(users, products, orders, sales, reset);
    }
}
