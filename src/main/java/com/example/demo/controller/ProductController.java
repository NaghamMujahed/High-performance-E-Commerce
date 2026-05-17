package com.example.demo.controller;

import com.example.demo.model.Product;
import com.example.demo.service.ProductService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/products")
public class ProductController {

    private final ProductService service;

    public ProductController(ProductService service) {
        this.service = service;
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
}
