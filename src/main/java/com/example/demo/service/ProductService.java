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

//2 Without Pool
    @Transactional
    public String purchaseWithoutPool(Long id, int quantity) {
        long start = System.currentTimeMillis();
        String thread = Thread.currentThread().getName();

        System.out.println(" Starting purchase WITHOUT pool...");
        System.out.println("   ─ Product ID: " + id);
        System.out.println("   ─ Quantity: " + quantity);
        System.out.println("   ─ Thread: " + thread);

        try {
            System.out.println("\n   [STEP 1] Fetching product with lock...");
            Product product = repository.findByIdWithLock(id)
                    .orElseThrow(() -> {
                        System.err.println("  Product not found: " + id);
                        return new RuntimeException("Product not found");
                    });
            System.out.println("   ─ Found: " + product.getName());
            System.out.println("   ─ Available: " + product.getQuantity());
            System.out.println("   ─ Price: $" + product.getPrice());

            System.out.println("\n   [STEP 2] Checking stock...");
            if (product.getQuantity() < quantity) {
                System.out.println("    FAILED: Insufficient stock!");
                return "ERROR: Insufficient stock. Available: " + product.getQuantity() +
                        ", Requested: " + quantity + " | Thread: " + thread;
            }
            System.out.println("    Stock OK");

            System.out.println("\n   [STEP 3] Deducting stock...");
            product.setQuantity(product.getQuantity() - quantity);
            repository.save(product);
            System.out.println("    New quantity: " + product.getQuantity());

            System.out.println("\n   [STEP 4] Simulating slow operation (email)...");
            Thread.sleep(1500);
            System.out.println("    Email sent");

            long end = System.currentTimeMillis();
            System.out.println("\n    Purchase completed in " + (end - start) + "ms");

            return "SUCCESS: Purchased " + quantity + " x " + product.getName() +
                    " | Remaining: " + product.getQuantity() +
                    " | Time: " + (end - start) + "ms" +
                    " | Thread: " + thread;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "ERROR: Interrupted | Thread: " + thread;
        } catch (Exception e) {
            System.err.println("    Error: " + e.getMessage());
            return "ERROR: " + e.getMessage() + " | Thread: " + thread;
        }
    }
    //2 With Pool
    @Transactional
    @Async("taskExecutor")
    public CompletableFuture<String> purchaseWithPool(Long id, int quantity) {
        long start = System.currentTimeMillis();
        String thread = Thread.currentThread().getName();

        System.out.println(" Starting purchase WITH pool...");
        System.out.println("   ─ Product ID: " + id);
        System.out.println("   ─ Quantity: " + quantity);
        System.out.println("   ─ Thread: " + thread);

        try {
            // 1. جلب المنتج مع قفل
            System.out.println("\n   [STEP 1] Fetching product with lock...");
            Product product = repository.findByIdWithLock(id)
                    .orElseThrow(() -> {
                        System.err.println("    Product not found: " + id);
                        return new RuntimeException("Product not found");
                    });
            System.out.println("   ─ Found: " + product.getName());
            System.out.println("   ─ Available: " + product.getQuantity());
            System.out.println("   ─ Price: $" + product.getPrice());

            // 2. التحقق من الكمية
            System.out.println("\n   [STEP 2] Checking stock...");
            if (product.getQuantity() < quantity) {
                System.out.println("    FAILED: Insufficient stock!");
                return CompletableFuture.completedFuture(
                        "ERROR: Insufficient stock. Available: " + product.getQuantity() +
                                ", Requested: " + quantity + " | Thread: " + thread);
            }
            System.out.println("    Stock OK");

            // 3. خصم الكمية
            System.out.println("\n   [STEP 3] Deducting stock...");
            product.setQuantity(product.getQuantity() - quantity);
            repository.save(product);
            System.out.println("    New quantity: " + product.getQuantity());

            // 4. عملية بطيئة (محاكاة إرسال إيميل)
            System.out.println("\n   [STEP 4] Simulating slow operation (email)...");
            Thread.sleep(1500); // 1.5 ثانية
            System.out.println("    Email sent");

            long end = System.currentTimeMillis();
            System.out.println("\n    Purchase completed in " + (end - start) + "ms");

            return CompletableFuture.completedFuture(
                    "SUCCESS: Purchased " + quantity + " x " + product.getName() +
                            " | Remaining: " + product.getQuantity() +
                            " | Time: " + (end - start) + "ms" +
                            " | Thread: " + thread);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return CompletableFuture.completedFuture(
                    "ERROR: Interrupted | Thread: " + thread);
        } catch (Exception e) {
            System.err.println("    Error: " + e.getMessage());
            return CompletableFuture.completedFuture(
                    "ERROR: " + e.getMessage() + " | Thread: " + thread);
        }
    }}




