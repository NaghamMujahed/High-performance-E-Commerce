package com.example.demo.service;

import com.example.demo.model.Product;
import com.example.demo.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class ProductService {

    private final ProductRepository repository;
    private final NotificationService notificationService;

    public ProductService(ProductRepository repository , NotificationService notificationService) {
        this.repository = repository;
        this.notificationService = notificationService;
    }

    public List<Product> getAllProducts() {
        return repository.findAll();
    }

    public Product addProduct(Product product) {
        return repository.save(product);
    }

    public Product purchaseWithoutLock(Long id, int quantity) {
        Product product = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("product not found"));
        product.setQuantity(product.getQuantity() - quantity);
        return repository.save(product);
    }

    @Transactional
    public Product purchaseWithLock(Long id, int quantity) {
        Product product = repository.findByIdWithLock(id)
                .orElseThrow(() -> new RuntimeException("product not found"));

        if (product.getQuantity() < quantity) {
            throw new RuntimeException("quantity not sufficient");
        }

        product.setQuantity(product.getQuantity() - quantity);
        return repository.save(product);
    }

    public Product purchaseSync(Long id, int quantity) {
        Product product = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("product not found"));
        product.setQuantity(product.getQuantity() - quantity);
        Product saved = repository.save(product);
        notificationService.sendEmailSync(product.getName());
        return saved;
    }

    @Transactional
    public Product purchaseAsync(Long id, int quantity) {
        Product product = repository.findByIdWithLock(id)
                .orElseThrow(() -> new RuntimeException("product not found"));
        product.setQuantity(product.getQuantity() - quantity);
        Product saved = repository.save(product);
        notificationService.sendEmailAsync(product.getName()); // بالخلفية فوراً
        return saved;
    }

    public void deleteAll() {
        repository.deleteAll();
    }

    @Transactional
    public Product updateStock(Long id, int quantity) {
        Product product = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("product not found"));
        product.setQuantity(quantity);
        return repository.save(product);
    }
}