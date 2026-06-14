package com.example.demo.service;

import com.example.demo.dto.ProductDetailsResponse;
import com.example.demo.dto.UpdateProductRequest;
import com.example.demo.exception.ProductNotFoundException;
import com.example.demo.mapper.ProductMapper;
import com.example.demo.model.OrderStatus;
import com.example.demo.model.Product;
import com.example.demo.repository.ProductRepository;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository repository;
    private final NotificationService notificationService;
    private final PaymentService paymentService;

    // Cache Attributes
    private RedisTemplate<String, List<ProductDetailsResponse>> redisTemplate;
    private StringRedisTemplate stringRedisTemplate;
    private MeterRegistry meterRegistry;

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    public ProductService(ProductRepository repository,
            NotificationService notificationService,
            PaymentService paymentService,
            MeterRegistry meterRegistry,
            StringRedisTemplate stringRedisTemplate,
            RedisTemplate<String, List<ProductDetailsResponse>> redisTemplate) {
        this.repository = repository;
        this.notificationService = notificationService;
        this.paymentService = paymentService;
        this.meterRegistry = meterRegistry;
        this.redisTemplate = redisTemplate;
        this.stringRedisTemplate = stringRedisTemplate;
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

    // ************************** Cache Redis Methods *************************** //

    // This method for handling redis failure.
    public List<ProductDetailsResponse> safeGetTopProductDetails(int limit) {
        try {
            return getTopProductDetailsByCache(limit);
        } catch (RedisConnectionFailureException ex) {
            meterRegistry.counter("ecommerce.redis.fallback", "cache", "topProducts").increment();
            log.warn("Redis unavailable, falling back to DB for top products limit={}", limit);
            return loadTopProductsFromDb(limit);
        }
    }

    @SuppressWarnings("unchecked")
    public List<ProductDetailsResponse> getTopProductDetailsByCache(int limit) {
        String key = "topProducts:" + limit;

        Timer.Sample redisGetSample = Timer.start(meterRegistry);

        Object cached = null;
        try {
            cached = redisTemplate.opsForValue().get(key);
        } finally {
            redisGetSample.stop(Timer.builder("ecommerce.redis.latency")
                    .tag("operation", "GET")
                    .tag("cache", "topProducts")
                    .register(meterRegistry));
        }

        if (cached != null && cached instanceof List<?> response) {
            meterRegistry.counter("ecommerce.cache.hit", "cache", "topProducts").increment();
            log.info("CACHE_HIT cache=topProducts key={}", key);

            return (List<ProductDetailsResponse>) response;
        }

        meterRegistry.counter("ecommerce.cache.miss", "cache", "topProducts").increment();
        log.info("CACHE_MISS cache=topProducts key={}", key);

        String lockKey = "lock:" + key;
        Boolean lockAcquired = stringRedisTemplate.opsForValue()
                .setIfAbsent(lockKey, "1", Duration.ofSeconds(5));

        if (Boolean.TRUE.equals(lockAcquired)) {
            try {
                List<ProductDetailsResponse> loaded = loadTopProductsFromDb(limit);

                Duration ttlWithJitter = Duration.ofMinutes(5)
                        .plusSeconds(ThreadLocalRandom.current().nextInt(0, 30));

                Timer.Sample redisSetSample = Timer.start(meterRegistry);

                try {
                    redisTemplate.opsForValue().set(key, loaded, ttlWithJitter);
                } finally {
                    redisSetSample.stop(Timer.builder("ecommerce.redis.latency")
                            .tag("operation", "SET")
                            .tag("cache", "topProducts")
                            .register(meterRegistry));
                }

                return loaded;
            } finally {
                stringRedisTemplate.delete(lockKey);
            }
        }

        meterRegistry.counter("ecommerce.cache.lock_wait", "cache", "topProducts").increment();
        /*
         * اذا لم يحصل هذا الطلب على قفل.
         * ننتظر قليلاً ثم نحاول قراءة الكاش مرة أخرى.
         */
        sleepShortly();

        Object afterWait = redisTemplate.opsForValue().get(key);
        if (afterWait instanceof List<?> response) {
            meterRegistry.counter("ecommerce.cache.hit_after_wait", "cache", "topProducts").increment();
            return (List<ProductDetailsResponse>) response;
        }

        /*
         * fallback: نقرأ من قاعدة البيانات بشكل مباشر حتى لا يتعطل الطلب.
         */
        meterRegistry.counter("ecommerce.cache.fallback_to_db_after_wait", "cache", "topProducts").increment();

        return loadTopProductsFromDb(limit);
    }

    public List<ProductDetailsResponse> loadTopProductsFromDb(int limit) {
        meterRegistry.counter("ecommerce.db.query", "query", "findTopSellingProducts").increment();

        Timer.Sample dbSample = Timer.start(meterRegistry);

        try {
            return repository.findTopSellingProducts(OrderStatus.DELIVERED, PageRequest.of(0, limit))
                    .stream()
                    .map(ProductMapper::toDetailsRecord)
                    .toList();

        } finally {
            dbSample.stop(Timer.builder("ecommerce.db.query.latency")
                    .tag("query", "findTopSellingProducts")
                    .register(meterRegistry));
        }
    }

    private void sleepShortly() {
        try {
            Thread.sleep(250);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Transactional
    // @CacheEvict(cacheNames = "topProducts", allEntries = true)
    public void updateProduct(Long productId, UpdateProductRequest request) {
        Product product = repository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));

        product.setName(request.name());
        product.setDescription(request.description());
        product.setPrice(request.price());

        evictTopProductsCache();
    }

    private void evictTopProductsCache() {
        Set<String> keys = redisTemplate.keys("topProducts:*");

        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
            log.info("Evicted top products cache keys={}", keys.size());
        }
    }
    // ********************** End: Cache Redis Methods ************************* //
}
