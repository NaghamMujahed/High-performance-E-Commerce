package com.example.demo.controller;

import com.example.demo.dto.ProductDetailsResponse;
import com.example.demo.dto.UpdateProductRequest;
import com.example.demo.model.Product;
import com.example.demo.model.Server;
import com.example.demo.service.LoadBalancerService;
import com.example.demo.service.ProductRequestQueue;
import com.example.demo.service.ProductService;
import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

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
    private final ProductRequestQueue productRequestQueue;

    public ProductController(ProductService service, RestTemplate restTemplate,
            LoadBalancerService loadBalancerService, ProductRequestQueue productRequestQueue) {
        this.service = service;
        this.restTemplate = restTemplate;
        this.loadBalancerService = loadBalancerService;
        this.productRequestQueue = productRequestQueue;
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

        if (role.equals("loadbalancer")) {
            return productRequestQueue.executeForProduct(id, () -> forwardBuyAsync(id, quantity, userId));
        }
        return service.buyWithPaymentAsync(id, quantity, userId);
    }

    private Product forwardBuyAsync(Long id, int quantity, Long userId) {
        Server server = loadBalancerService.getBestServer();

        try {
            String url = UriComponentsBuilder
                    .fromHttpUrl(server.getUrl() + "/products/" + id + "/buy-async")
                    .queryParam("quantity", quantity)
                    .queryParam("userId", userId)
                    .toUriString();

            return restTemplate.postForObject(url, null, Product.class);
        } catch (ResourceAccessException ex) {
            throw new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT, "Worker did not respond in time", ex);
        } catch (RestClientResponseException ex) {
            throw new ResponseStatusException(ex.getStatusCode(), ex.getResponseBodyAsString(), ex);
        } catch (RestClientException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Worker request failed", ex);
        } finally {
            loadBalancerService.releaseServer(server);
        }
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
            @RequestParam(defaultValue = "10") int limit, @RequestParam(defaultValue = "true") boolean withLock) {

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
        return service.safeGetTopProductDetails(limit, withLock);
    }

    @PutMapping("/{productId}")
    public void updateProduct(
            @PathVariable Long productId,
            @Valid @RequestBody UpdateProductRequest request) {
        service.updateProduct(productId, request);
    }

    // ********************** End: Cache Redis Methods ************************* //
}
