package com.example.demo.service;

import com.example.demo.model.Product;
import com.example.demo.repository.ProductRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class ProductService {

    private final ProductRepository repository;
    private final NotificationService notificationService;
    private final PaymentService paymentService;

    public ProductService(ProductRepository repository,
                          NotificationService notificationService,
                          PaymentService paymentService) {
        this.repository = repository;
        this.notificationService = notificationService;
        this.paymentService = paymentService;
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
        notificationService.sendEmailAsync(product.getName());
        return saved;
    }

    @Transactional
    public Product buyWithPaymentSync(Long id, int quantity, Long userId) {
        Product product = repository.findByIdWithLock(id)
                .orElseThrow(() -> new RuntimeException("product not found"));

        if (product.getQuantity() < quantity) {
            throw new RuntimeException("quantity not sufficient");
        }
        double totalAmount = product.getPrice() * quantity;

        paymentService.processPaymentSync(userId, totalAmount);
        product.setQuantity(product.getQuantity() - quantity);
        return repository.save(product);
    }

    @Transactional
    public Product buyWithPaymentAsync(Long id, int quantity, Long userId) {
        Product product = repository.findByIdWithLock(id)
                .orElseThrow(() -> new RuntimeException("product not found"));

        if (product.getQuantity() < quantity) {
            throw new RuntimeException("quantity not sufficient");
        }
        double totalAmount = product.getPrice() * quantity;

        paymentService.processPaymentAsync(userId, totalAmount);

        product.setQuantity(product.getQuantity() - quantity);
        Product saved = repository.save(product);

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

    @Transactional
    public String purchaseWithoutVirtual(Long id, int quantity) {
        Product product = repository.findByIdWithLock(id)
                .orElseThrow(() -> new RuntimeException("Product not found: " + id));

        if (product.getQuantity() < quantity) {
            return "ERROR: Insufficient stock. Available: " + product.getQuantity() +
                    ", Requested: " + quantity;
        }

        product.setQuantity(product.getQuantity() - quantity);
        repository.save(product);

        try {
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "ERROR: Operation interrupted";
        }

        return "SUCCESS: Purchased " + quantity + " x " + product.getName() +
                " | Remaining: " + product.getQuantity();
    }

    @Transactional
    @Async
    public CompletableFuture<String> purchaseWithVirtual(Long id, int quantity) {

        Product product = repository.findByIdWithLock(id)
                .orElseThrow(() -> new RuntimeException("Product not found: " + id));

        if (product.getQuantity() < quantity) {
            return CompletableFuture.completedFuture(
                    "ERROR: Insufficient stock. Available: " + product.getQuantity() +
                            ", Requested: " + quantity);
        }

        product.setQuantity(product.getQuantity() - quantity);
        repository.save(product);

        try {
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return CompletableFuture.completedFuture("ERROR: Operation interrupted");
        }

        return CompletableFuture.completedFuture(
                "SUCCESS: Purchased " + quantity + " x " + product.getName() +
                        " | Remaining: " + product.getQuantity());
    }
    }




