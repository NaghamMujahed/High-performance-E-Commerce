package com.example.demo.controller;

import com.example.demo.dto.ProductDetailsResponse;
import com.example.demo.dto.UpdateProductRequest;
import com.example.demo.model.Product;
import com.example.demo.model.Server;
import com.example.demo.service.LoadBalancerService;
import com.example.demo.service.ProductService;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/products")
public class ProductController {

    @Value("${APP_ROLE:worker}")
    private String role;

    private LoadBalancerService loadBalancerService;
    private RestTemplate restTemplate;

    private final ProductService service;

    public ProductController(ProductService service, RestTemplate restTemplate,
            LoadBalancerService loadBalancerService) {
        this.service = service;
        this.restTemplate = restTemplate;
        this.loadBalancerService = loadBalancerService;
    }

    @GetMapping
    public List<Product> getAllProducts() {
        return service.getAllProducts();
    }

    @PostMapping
    public Product addProduct(@RequestBody Product product) {
        return service.addProduct(product);
    }

    @PostMapping("/{id}/purchase-unsafe")
    public Product purchaseUnsafe(@PathVariable Long id,
            @RequestParam int quantity) {
        return service.purchaseWithoutLock(id, quantity);
    }

    @PostMapping("/{id}/purchase")
    public Product purchaseSafe(@PathVariable Long id,
            @RequestParam int quantity) {
        return service.purchaseWithLock(id, quantity);
    }

    @PostMapping("/{id}/purchase-sync")
    public Product purchaseSync(@PathVariable Long id,
            @RequestParam int quantity) {
        return service.purchaseSync(id, quantity);
    }

    @PostMapping("/{id}/purchase-async")
    public Product purchaseAsync(@PathVariable Long id,
            @RequestParam int quantity) {
        return service.purchaseAsync(id, quantity);
    }

    @PostMapping("/{id}/buy-sync")
    public Product buySync(@PathVariable Long id,
            @RequestParam int quantity,
            @RequestParam Long userId) {
        return service.buyWithPaymentSync(id, quantity, userId);
    }

    @PostMapping("/{id}/buy-async")
    public Product buyAsync(@PathVariable Long id,
            @RequestParam int quantity,
            @RequestParam Long userId) {
        return service.buyWithPaymentAsync(id, quantity, userId);
    }

    @DeleteMapping
    public void deleteAll() {
        service.deleteAll();
    }

    @PutMapping("/{id}/stock")
    public Product updateStock(@PathVariable Long id,
            @RequestParam int quantity) {
        return service.updateStock(id, quantity);
    }

    @PostMapping("/without-Virtual/{id}")
    public String purchaseWithoutPool(@PathVariable Long id, @RequestParam int quantity) {
        return service.purchaseWithoutVirtual(id, quantity);
    }

    @PostMapping("/with-Virtual/{id}")
    public String purchaseWithPool(@PathVariable Long id, @RequestParam int quantity) {
        return service.purchaseWithVirtual(id, quantity).join();
    }

    // ********************** Cache Redis Methods ************************* //

    @GetMapping("/top-selling/without-cache")
    public List<ProductDetailsResponse> getTopSellingWithoutCache(
            @RequestParam(defaultValue = "10") int limit) {
        if (role.equals("loadbalancer")) {
            Server server = loadBalancerService.getBestServer();

            try {
                String url = server.getUrl() + "/products/top-selling/without-cache?limit=" + limit;

                ProductDetailsResponse[] response = restTemplate.getForObject(url, ProductDetailsResponse[].class);

                return response == null ? List.of() : Arrays.asList(response);
            } finally {
                loadBalancerService.releaseServer(server);
            }
        }
        return service.loadTopProductsFromDb(limit);
    }

    @GetMapping("/top-selling/by-cache")
    public List<ProductDetailsResponse> getTopSellingByCache(
            @RequestParam(defaultValue = "10") int limit) {

        if (role.equals("loadbalancer")) {
            Server server = loadBalancerService.getBestServer();

            try {
                String url = server.getUrl() + "/products/top-selling/by-cache?limit=" + limit;

                ProductDetailsResponse[] response = restTemplate.getForObject(url, ProductDetailsResponse[].class);

                return response == null ? List.of() : Arrays.asList(response);
            } finally {
                loadBalancerService.releaseServer(server);
            }
        }
        return service.safeGetTopProductDetails(limit);
    }

    @PutMapping("/{productId}")
    public void updateProduct(
            @PathVariable Long productId,
            @Valid @RequestBody UpdateProductRequest request) {
        service.updateProduct(productId, request);
    }

    // ********************** End: Cache Redis Methods ************************* //
}
