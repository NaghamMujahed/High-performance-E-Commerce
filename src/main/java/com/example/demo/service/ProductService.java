package com.example.demo.service;

import com.example.demo.dto.ProductDetailsResponse;
import com.example.demo.dto.UpdateProductRequest;
import com.example.demo.exception.ProductNotFoundException;
import com.example.demo.exception.QuantityNotSufficient;
import com.example.demo.mapper.ProductMapper;
import com.example.demo.model.OrderStatus;
import com.example.demo.model.Product;
import com.example.demo.repository.ProductRepository;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository repository;
    private final NotificationService notificationService;
    private final PaymentService paymentService;

    // Cache Attributes for sixth requirment.
    private RedisTemplate<String, List<ProductDetailsResponse>> redisTemplate;
    private StringRedisTemplate stringRedisTemplate;
    private MeterRegistry meterRegistry;

    // Redisson lock attribute for seventh requirment.
    private RedissonClient redissonClient;

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    public ProductService(ProductRepository repository,
            NotificationService notificationService,
            PaymentService paymentService,
            MeterRegistry meterRegistry,
            StringRedisTemplate stringRedisTemplate,
            RedisTemplate<String, List<ProductDetailsResponse>> redisTemplate,
            RedissonClient redissonClient) {
        this.repository = repository;
        this.notificationService = notificationService;
        this.paymentService = paymentService;
        this.meterRegistry = meterRegistry;
        this.redisTemplate = redisTemplate;
        this.stringRedisTemplate = stringRedisTemplate;
        this.redissonClient = redissonClient;
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
            throw new QuantityNotSufficient();
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

        if (product.getQuantity() < quantity)
            throw new QuantityNotSufficient();

        double totalAmount = product.getPrice() * quantity;

        paymentService.processPaymentSync(userId, totalAmount);
        product.setQuantity(product.getQuantity() - quantity);
        return repository.save(product);
    }

    @Transactional
    public Product buyWithPaymentAsync(Long id, int quantity, Long userId) {
        Product product = repository.findByIdWithLock(id)
                .orElseThrow(() -> new RuntimeException("product not found"));

        if (product.getQuantity() < quantity)
            throw new QuantityNotSufficient();

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
    public List<ProductDetailsResponse> safeGetTopProductDetails(int limit, boolean withLock) {
        try {
            if (withLock)
                return getTopProductDetailsByCacheWithLock(limit, withLock);

            return getTopProductDetailsByCacheWithoutLock(limit, withLock);

        } catch (RedisConnectionFailureException ex) {
            meterRegistry.counter("ecommerce.redis.fallback", "cache", "topProducts").increment();
            log.warn("Redis unavailable, falling back to DB for top products limit={}", limit);
            return loadTopProductsFromDb(limit);
        }
    }

    public List<ProductDetailsResponse> getTopProductDetailsByCacheWithLock(int limit, boolean withLock) {
        String freshKey = freshKey(limit, withLock);
        String staleKey = staleKey(limit, withLock);
        String lockKey = lockKey(limit);

        List<ProductDetailsResponse> fresh = readListFromRedis(freshKey);

        if (fresh != null) {
            incrementCacheMetric("app.cache.hit", "topProducts");

            return fresh;
        }

        incrementCacheMetric("app.cache.miss", "topProducts");

        RLock lock = redissonClient.getLock(lockKey);

        boolean acquired = false;
        Timer.Sample waitSample = Timer.start(meterRegistry);

        try {
            acquired = lock.tryLock(150, 10, TimeUnit.MILLISECONDS); // waitTime = 150ms, leaseTime = 10s

            waitSample.stop(Timer.builder("app.distributed_lock.wait")
                    .tag("lock", "topProductsRebuild")
                    .register(meterRegistry));

            if (acquired) {
                incrementLockMetric("app.distributed_lock.acquired", "topProductsRebuild");

                return rebuildCacheWithDoubleCheck(limit, freshKey, staleKey, withLock);
            }

            incrementLockMetric("app.distributed_lock.skipped", "topProductsRebuild");

            List<ProductDetailsResponse> stale = readListFromRedis(staleKey);

            if (stale != null) {
                incrementCacheMetric("app.cache.stale_served", "topProducts");
                return stale;
            }

            sleepBriefly();

            List<ProductDetailsResponse> afterWait = readListFromRedis(freshKey);

            if (afterWait != null) {
                incrementCacheMetric("app.cache.hit_after_wait", "topProducts");
                return afterWait;
            }

            incrementCacheMetric("app.cache.fallback_db", "topProducts");

            // Fallback أخير:
            // نقرأ قاعدة البيانات حتى لا يفشل الطلب، لكن لا نعيد بناء الكاش هنا
            // لأننا لم نملك قفل.
            return loadTopProductsFromDb(limit);

        } catch (Exception ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for top products lock", ex);

        } finally {
            if (acquired && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    public List<ProductDetailsResponse> getTopProductDetailsByCacheWithoutLock(int limit, boolean withLock) {
        String freshKey = freshKey(limit, withLock);
        String staleKey = staleKey(limit, withLock);

        List<ProductDetailsResponse> fresh = readListFromRedis(freshKey);

        if (fresh != null) {
            incrementCacheMetric("app.cache.hit", "topProducts");

            return fresh;
        }

        incrementCacheMetric("app.cache.miss", "topProducts");

        try {
            return rebuildCacheWithDoubleCheck(limit, freshKey, staleKey, withLock);
        } catch (Exception ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for top products lock", ex);
        }
    }

    private List<ProductDetailsResponse> rebuildCacheWithDoubleCheck(int limit, String freshKey, String staleKey,
            boolean withLock) {

        // Double-check:
        // ربما thread آخر بنى الكاش بين miss وأخذ lock.
        List<ProductDetailsResponse> existing = readListFromRedis(freshKey);

        if (existing != null && withLock) {
            incrementCacheMetric("app.cache.hit_after_lock", "topProducts");
            return existing;
        }

        Timer.Sample rebuildSample = Timer.start(meterRegistry);

        List<ProductDetailsResponse> loaded = loadTopProductsFromDb(limit);

        rebuildSample.stop(Timer.builder("app.cache.rebuild.duration")
                .tag("cache", "topProducts")
                .register(meterRegistry));

        if (withLock) {
            Duration freshTtl = Duration.ofSeconds(90)
                    .plusSeconds(ThreadLocalRandom.current().nextInt(0, 10));

            Duration staleTtl = Duration.ofMinutes(10);

            redisTemplate.opsForValue().set(freshKey, loaded, freshTtl);
            redisTemplate.opsForValue().set(staleKey, loaded, staleTtl);
        } else {
            Duration freshTtl = Duration.ofMinutes(5)
                    .plusSeconds(ThreadLocalRandom.current().nextInt(0, 10));

            redisTemplate.opsForValue().set(freshKey, loaded, freshTtl);
        }
        incrementCacheMetric("app.cache.rebuild.success", "topProducts");

        return loaded;
    }

    public List<ProductDetailsResponse> loadTopProductsFromDb(int limit) {
        meterRegistry.counter("app.db.query", "query", "findTopSellingProducts").increment();

        Timer.Sample dbSample = Timer.start(meterRegistry);

        try {
            log.info("DB_QUERY topProducts limit={}", limit);

            return repository.findTopSellingProducts(OrderStatus.DELIVERED, PageRequest.of(0, limit))
                    .stream()
                    .map(ProductMapper::toDetailsRecord)
                    .toList();
        } finally {
            dbSample.stop(Timer.builder("app.db.query.duration")
                    .tag("query", "findTopSellingProducts")
                    .register(meterRegistry));
        }
    }

    @SuppressWarnings("unchecked")
    private List<ProductDetailsResponse> readListFromRedis(String key) {
        Object value = redisTemplate.opsForValue().get(key);

        if (value == null) {
            return null;
        }

        return (List<ProductDetailsResponse>) value;
    }

    private String freshKey(int limit, boolean withLock) {
        if (withLock)
            return "top-products:limit:with-lock:" + limit;

        return "top-products:limit:without-lock:" + limit;
    }

    private String staleKey(int limit, boolean withLock) {
        if (withLock)
            return "top-products:stale:limit:with-lock:" + limit;

        return "top-products:stale:limit:without-lock:" + limit;
    }

    private String lockKey(int limit) {
        return "lock:cache-rebuild:top-products:limit:" + limit;
    }

    private void incrementCacheMetric(String metricName, String cacheName) {
        meterRegistry.counter(metricName, "cache", cacheName).increment();
    }

    private void incrementLockMetric(String metricName, String lockName) {
        meterRegistry.counter(metricName, "lock", lockName).increment();
    }

    private void sleepBriefly() {
        try {
            Thread.sleep(150);
        } catch (InterruptedException ex) {
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
        Set<String> keys = redisTemplate.keys("top-products:*");

        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
            log.info("Evicted top products cache keys={}", keys.size());
        }
    }
    // ********************** End: Cache Redis Methods ************************* //
}
