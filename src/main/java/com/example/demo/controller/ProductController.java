package com.example.demo.controller;

import com.example.demo.model.Product;
import com.example.demo.service.ProductService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

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

    @DeleteMapping
    public void deleteAll() {
        service.deleteAll();
    }
}