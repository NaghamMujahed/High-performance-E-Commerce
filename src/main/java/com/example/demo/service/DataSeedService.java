package com.example.demo.service;

import com.example.demo.model.*;
import com.example.demo.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Service
public class DataSeedService {
    private static final Logger logger = LoggerFactory.getLogger(DataSeedService.class);
    private static final int SAVE_BATCH_SIZE = 500;
    private static final int ORDER_BATCH_SIZE = 250;

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final SaleRepository saleRepository;
    private final CategoryRepository categoryRepository;
    private final CustomerAddressRepository addressRepository;
    private final CustomerOrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PaymentRepository paymentRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;
    private final ProductReviewRepository productReviewRepository;
    private final Random random = new Random(20260612L);

    public DataSeedService(UserRepository userRepository,
            ProductRepository productRepository,
            SaleRepository saleRepository,
            CategoryRepository categoryRepository,
            CustomerAddressRepository addressRepository,
            CustomerOrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            PaymentRepository paymentRepository,
            InventoryTransactionRepository inventoryTransactionRepository,
            ProductReviewRepository productReviewRepository) {
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.saleRepository = saleRepository;
        this.categoryRepository = categoryRepository;
        this.addressRepository = addressRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.paymentRepository = paymentRepository;
        this.inventoryTransactionRepository = inventoryTransactionRepository;
        this.productReviewRepository = productReviewRepository;
    }

    @Transactional
    public synchronized SeedResult seed(int userCount, int productCount, int orderCount, int saleCount, boolean reset) {
        if (reset) {
            clearGeneratedData();
        }

        List<Category> categories = ensureCategories();
        int usersCreated = ensureUsers(userCount);
        List<User> users = userRepository.findAll();

        int productsCreated = ensureProducts(productCount, categories);
        List<Product> products = productRepository.findAll();

        int addressesCreated = ensureAddresses(users);
        int inventoryCreated = ensureInitialInventory(products);
        int ordersCreated = ensureOrders(orderCount, users, products);
        int salesCreated = ensureLegacySales(saleCount, users, products);
        int reviewsCreated = ensureReviews(users, products);

        SeedResult result = new SeedResult(
                categoryRepository.count(),
                userRepository.count(),
                productRepository.count(),
                addressRepository.count(),
                orderRepository.count(),
                orderItemRepository.count(),
                paymentRepository.count(),
                inventoryTransactionRepository.count(),
                productReviewRepository.count(),
                saleRepository.count(),
                usersCreated,
                productsCreated,
                addressesCreated,
                inventoryCreated,
                ordersCreated,
                salesCreated,
                reviewsCreated);

        logger.info("[SEED] {}", result);
        return result;
    }

    private void clearGeneratedData() {
        productReviewRepository.deleteAllInBatch();
        inventoryTransactionRepository.deleteAllInBatch();
        paymentRepository.deleteAllInBatch();
        orderItemRepository.deleteAllInBatch();
        orderRepository.deleteAllInBatch();
        addressRepository.deleteAllInBatch();
        saleRepository.deleteAllInBatch();
        productRepository.deleteAllInBatch();
        categoryRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
    }

    private List<Category> ensureCategories() {
        String[][] definitions = {
                { "Electronics", "Phones, laptops, accessories, and connected devices." },
                { "Home Appliances", "Kitchen, cleaning, and home comfort devices." },
                { "Fashion", "Clothing, shoes, watches, and daily accessories." },
                { "Books", "Technical, business, and personal development books." },
                { "Sports", "Training equipment, outdoor gear, and fitness accessories." },
                { "Beauty", "Skin care, grooming, and personal care products." },
                { "Gaming", "Consoles, games, controllers, and streaming gear." },
                { "Office", "Desk equipment, stationery, and productivity tools." }
        };

        for (String[] definition : definitions) {
            categoryRepository.findByName(definition[0]).orElseGet(() -> {
                Category category = new Category();
                category.setName(definition[0]);
                category.setDescription(definition[1]);
                return categoryRepository.save(category);
            });
        }
        return categoryRepository.findAll();
    }

    private int ensureUsers(int targetCount) {
        long existing = userRepository.count();
        int missing = positiveDifference(targetCount, existing);
        if (missing == 0) {
            return 0;
        }

        List<User> users = new ArrayList<>(missing);
        int start = Math.toIntExact(existing) + 1;
        for (int i = start; i < start + missing; i++) {
            User user = new User();
            user.setName("Customer " + i);
            user.setEmail("customer%05d@example.com".formatted(i));
            user.setPassword("password");
            user.setBalance(1000000);
            user.setRole("CUSTOMER");
            user.setStatus(i % 25 == 0 ? "SUSPENDED" : "ACTIVE");
            user.setCreatedAt(randomDateTime(365));
            users.add(user);
        }
        saveAllInChunks(users, userRepository);
        return missing;
    }

    private int ensureProducts(int targetCount, List<Category> categories) {
        long existing = productRepository.count();
        int missing = positiveDifference(targetCount, existing);
        if (missing == 0) {
            return 0;
        }

        String[] brands = { "Nova", "Orion", "Pulse", "Apex", "Atlas", "Zen", "Vertex", "Nimbus" };
        String[] productWords = { "Pro", "Max", "Lite", "Air", "Core", "Flex", "Prime", "Edge" };
        List<Product> products = new ArrayList<>(missing);
        int start = Math.toIntExact(existing) + 1;

        for (int i = start; i < start + missing; i++) {
            Category category = pick(categories);
            String brand = brands[random.nextInt(brands.length)];
            Product product = new Product();
            product.setSku("SKU-%06d".formatted(i));
            product.setName(category.getName() + " " + brand + " " + productWords[random.nextInt(productWords.length)]
                    + " " + i);
            product.setDescription("Seed product for load, query, inventory, and checkout experiments.");
            product.setBrand(brand);
            product.setPrice(round(5 + random.nextDouble() * 95));
            product.setQuantity(2000);
            product.setReservedQuantity(random.nextInt(25));
            product.setStatus(ProductStatus.ACTIVE);
            product.setCategory(category);
            product.setCreatedAt(randomDateTime(300));
            products.add(product);
        }

        saveAllInChunks(products, productRepository);
        return missing;
    }

    private int ensureAddresses(List<User> users) {
        if (users.isEmpty() || addressRepository.count() > 0) {
            return 0;
        }

        String[] cities = { "Damascus", "Aleppo", "Homs", "Latakia", "Tartus", "Hama", "Daraa", "Sweida" };
        List<CustomerAddress> addresses = new ArrayList<>(users.size() * 2);

        for (User user : users) {
            addresses.add(createAddress(user, AddressType.SHIPPING, cities, true));
            if (user.getId() % 3 == 0) {
                addresses.add(createAddress(user, AddressType.BILLING, cities, false));
            }
        }

        saveAllInChunks(addresses, addressRepository);
        return addresses.size();
    }

    private CustomerAddress createAddress(User user, AddressType type, String[] cities, boolean defaultAddress) {
        CustomerAddress address = new CustomerAddress();
        address.setUser(user);
        address.setType(type);
        address.setFullName(user.getName());
        address.setPhone("+9639%08d".formatted(10000000 + random.nextInt(89999999)));
        address.setCountry("Syria");
        address.setCity(cities[random.nextInt(cities.length)]);
        address.setStreet("Street " + (1 + random.nextInt(200)) + ", Building " + (1 + random.nextInt(80)));
        address.setPostalCode("%05d".formatted(10000 + random.nextInt(89999)));
        address.setDefaultAddress(defaultAddress);
        address.setCreatedAt(randomDateTime(365));
        return address;
    }

    private int ensureInitialInventory(List<Product> products) {
        if (products.isEmpty() || inventoryTransactionRepository.count() > 0) {
            return 0;
        }

        List<InventoryTransaction> transactions = new ArrayList<>(products.size());
        for (Product product : products) {
            InventoryTransaction transaction = new InventoryTransaction();
            transaction.setProduct(product);
            transaction.setType(InventoryTransactionType.INITIAL_STOCK);
            transaction.setQuantityChange(product.getQuantity());
            transaction.setStockAfter(product.getQuantity());
            transaction.setReferenceType("SEED");
            transaction.setReason("Initial generated stock");
            transaction.setCreatedAt(product.getCreatedAt());
            transactions.add(transaction);
        }
        saveAllInChunks(transactions, inventoryTransactionRepository);
        return transactions.size();
    }

    private int ensureOrders(int targetCount, List<User> users, List<Product> products) {
        if (users.isEmpty() || products.isEmpty()) {
            return 0;
        }

        long existing = orderRepository.count();
        int missing = positiveDifference(targetCount, existing);
        if (missing == 0) {
            return 0;
        }

        Map<Long, Integer> stockByProductId = new HashMap<>();
        for (Product product : products) {
            stockByProductId.put(product.getId(), product.getQuantity());
        }

        int created = 0;
        int sequenceStart = Math.toIntExact(existing) + 1;
        for (int offset = 0; offset < missing; offset += ORDER_BATCH_SIZE) {
            int currentBatchSize = Math.min(ORDER_BATCH_SIZE, missing - offset);
            List<CustomerOrder> orders = new ArrayList<>(currentBatchSize);

            for (int i = 0; i < currentBatchSize; i++) {
                int sequence = sequenceStart + offset + i;
                CustomerOrder order = new CustomerOrder();
                order.setOrderNumber("ORD-%08d".formatted(sequence));
                order.setCustomer(pick(users));
                order.setStatus(randomOrderStatus());
                order.setCreatedAt(randomDateTime(180));
                orders.add(order);
            }

            orderRepository.saveAll(orders);

            List<OrderItem> items = new ArrayList<>();
            List<Payment> payments = new ArrayList<>();
            List<InventoryTransaction> inventoryTransactions = new ArrayList<>();

            for (CustomerOrder order : orders) {
                BigDecimal subtotal = BigDecimal.ZERO;
                int itemCount = 1 + random.nextInt(5);

                for (int i = 0; i < itemCount; i++) {
                    Product product = pick(products);
                    int quantity = 1 + random.nextInt(4);
                    BigDecimal unitPrice = money(product.getPrice());
                    BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(quantity)).setScale(2,
                            RoundingMode.HALF_UP);
                    subtotal = subtotal.add(lineTotal);

                    OrderItem item = new OrderItem();
                    item.setOrder(order);
                    item.setProduct(product);
                    item.setProductNameSnapshot(product.getName());
                    item.setQuantity(quantity);
                    item.setUnitPrice(unitPrice);
                    item.setLineTotal(lineTotal);
                    item.setCreatedAt(order.getCreatedAt());
                    items.add(item);

                    if (affectsInventory(order.getStatus())) {
                        int stockAfter = stockByProductId.get(product.getId()) - quantity;
                        stockByProductId.put(product.getId(), stockAfter);

                        InventoryTransaction transaction = new InventoryTransaction();
                        transaction.setProduct(product);
                        transaction.setType(InventoryTransactionType.SALE);
                        transaction.setQuantityChange(-quantity);
                        transaction.setStockAfter(stockAfter);
                        transaction.setReferenceType("ORDER");
                        transaction.setReferenceId(order.getId());
                        transaction.setReason("Generated order sale");
                        transaction.setCreatedAt(order.getCreatedAt());
                        inventoryTransactions.add(transaction);
                    }
                }

                BigDecimal tax = subtotal.multiply(BigDecimal.valueOf(0.08)).setScale(2, RoundingMode.HALF_UP);
                BigDecimal shipping = random.nextBoolean() ? BigDecimal.ZERO
                        : BigDecimal.valueOf(5 + random.nextInt(20)).setScale(2);
                BigDecimal discount = random.nextInt(10) == 0
                        ? subtotal.multiply(BigDecimal.valueOf(0.10)).setScale(2, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO;
                BigDecimal total = subtotal.add(tax).add(shipping).subtract(discount).max(BigDecimal.ZERO).setScale(2,
                        RoundingMode.HALF_UP);

                order.setSubtotal(subtotal.setScale(2, RoundingMode.HALF_UP));
                order.setTaxAmount(tax);
                order.setShippingFee(shipping);
                order.setDiscountAmount(discount);
                order.setTotalAmount(total);

                Payment payment = new Payment();
                payment.setOrder(order);
                payment.setUser(order.getCustomer());
                payment.setMethod(randomPaymentMethod());
                payment.setStatus(paymentStatusFor(order.getStatus()));
                payment.setAmount(total);
                payment.setTransactionReference("PAY-%08d".formatted(order.getId()));
                payment.setCreatedAt(order.getCreatedAt().plusMinutes(random.nextInt(30)));
                if (payment.getStatus() != PaymentStatus.PENDING) {
                    payment.setProcessedAt(payment.getCreatedAt().plusSeconds(5 + random.nextInt(120)));
                }
                payments.add(payment);
            }

            orderRepository.saveAll(orders);
            saveAllInChunks(items, orderItemRepository);
            saveAllInChunks(payments, paymentRepository);
            saveAllInChunks(inventoryTransactions, inventoryTransactionRepository);
            created += currentBatchSize;
        }

        for (Product product : products) {
            Integer stock = stockByProductId.get(product.getId());
            if (stock != null) {
                product.setQuantity(Math.max(0, stock));
            }
        }
        saveAllInChunks(products, productRepository);

        return created;
    }

    private int ensureLegacySales(int targetCount, List<User> users, List<Product> products) {
        if (users.isEmpty() || products.isEmpty()) {
            return 0;
        }

        long existing = saleRepository.count();
        int missing = positiveDifference(targetCount, existing);
        if (missing == 0) {
            return 0;
        }

        List<Sale> sales = new ArrayList<>(Math.min(SAVE_BATCH_SIZE, missing));
        int start = Math.toIntExact(existing) + 1;

        for (int i = start; i < start + missing; i++) {
            User user = pick(users);
            Product product = pick(products);
            int quantity = 1 + random.nextInt(6);

            Sale sale = new Sale();
            sale.setProductId(product.getId());
            sale.setUserId(user.getId());
            sale.setOrderNumber("LEGACY-SALE-%08d".formatted(i));
            sale.setProductName(product.getName());
            sale.setQuantity(quantity);
            sale.setTotalPrice(round(product.getPrice() * quantity));
            sale.setSaleDate(LocalDate.now().minusDays(random.nextInt(365)));
            sale.setProcessed(random.nextDouble() < 0.15);
            sales.add(sale);

            if (sales.size() == SAVE_BATCH_SIZE) {
                saleRepository.saveAll(sales);
                sales.clear();
            }
        }

        if (!sales.isEmpty()) {
            saleRepository.saveAll(sales);
        }
        return missing;
    }

    private int ensureReviews(List<User> users, List<Product> products) {
        if (users.isEmpty() || products.isEmpty() || productReviewRepository.count() > 0) {
            return 0;
        }

        int target = Math.min(5_000, products.size() * 6);
        String[] titles = { "Great value", "Works as expected", "Fast delivery", "Good quality", "Would buy again" };
        List<ProductReview> reviews = new ArrayList<>(target);

        for (int i = 0; i < target; i++) {
            ProductReview review = new ProductReview();
            review.setUser(pick(users));
            review.setProduct(pick(products));
            review.setRating(3 + random.nextInt(3));
            review.setTitle(titles[random.nextInt(titles.length)]);
            review.setComment("Generated review useful for read-heavy listing and aggregation tests.");
            review.setApproved(random.nextDouble() > 0.03);
            review.setCreatedAt(randomDateTime(180));
            reviews.add(review);
        }

        saveAllInChunks(reviews, productReviewRepository);
        return reviews.size();
    }

    private OrderStatus randomOrderStatus() {
        double value = random.nextDouble();
        if (value < 0.08)
            return OrderStatus.CREATED;
        if (value < 0.18)
            return OrderStatus.CONFIRMED;
        if (value < 0.42)
            return OrderStatus.PAID;
        if (value < 0.62)
            return OrderStatus.SHIPPED;
        if (value < 0.92)
            return OrderStatus.DELIVERED;
        return OrderStatus.CANCELLED;
    }

    private PaymentMethod randomPaymentMethod() {
        PaymentMethod[] methods = PaymentMethod.values();
        return methods[random.nextInt(methods.length)];
    }

    private PaymentStatus paymentStatusFor(OrderStatus status) {
        return switch (status) {
            case CREATED -> PaymentStatus.PENDING;
            case CONFIRMED -> PaymentStatus.AUTHORIZED;
            case PAID, SHIPPED, DELIVERED -> PaymentStatus.CAPTURED;
            case CANCELLED -> PaymentStatus.FAILED;
        };
    }

    private boolean affectsInventory(OrderStatus status) {
        return status == OrderStatus.PAID
                || status == OrderStatus.SHIPPED
                || status == OrderStatus.DELIVERED;
    }

    private LocalDateTime randomDateTime(int daysBack) {
        return LocalDateTime.now()
                .minusDays(random.nextInt(daysBack + 1))
                .minusMinutes(random.nextInt(24 * 60));
    }

    private double round(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private BigDecimal money(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }

    private int positiveDifference(int target, long existing) {
        return Math.max(0, target - Math.toIntExact(Math.min(existing, Integer.MAX_VALUE)));
    }

    private <T> T pick(List<T> items) {
        return items.get(random.nextInt(items.size()));
    }

    private <T> void saveAllInChunks(List<T> items, JpaRepository<T, Long> repository) {
        for (int start = 0; start < items.size(); start += SAVE_BATCH_SIZE) {
            int end = Math.min(start + SAVE_BATCH_SIZE, items.size());
            repository.saveAll(items.subList(start, end));
        }
    }

    public static class SeedResult {
        private final long categories;
        private final long users;
        private final long products;
        private final long addresses;
        private final long orders;
        private final long orderItems;
        private final long payments;
        private final long inventoryTransactions;
        private final long reviews;
        private final long legacySales;
        private final int usersCreated;
        private final int productsCreated;
        private final int addressesCreated;
        private final int inventoryTransactionsCreated;
        private final int ordersCreated;
        private final int legacySalesCreated;
        private final int reviewsCreated;

        public SeedResult(long categories,
                long users,
                long products,
                long addresses,
                long orders,
                long orderItems,
                long payments,
                long inventoryTransactions,
                long reviews,
                long legacySales,
                int usersCreated,
                int productsCreated,
                int addressesCreated,
                int inventoryTransactionsCreated,
                int ordersCreated,
                int legacySalesCreated,
                int reviewsCreated) {
            this.categories = categories;
            this.users = users;
            this.products = products;
            this.addresses = addresses;
            this.orders = orders;
            this.orderItems = orderItems;
            this.payments = payments;
            this.inventoryTransactions = inventoryTransactions;
            this.reviews = reviews;
            this.legacySales = legacySales;
            this.usersCreated = usersCreated;
            this.productsCreated = productsCreated;
            this.addressesCreated = addressesCreated;
            this.inventoryTransactionsCreated = inventoryTransactionsCreated;
            this.ordersCreated = ordersCreated;
            this.legacySalesCreated = legacySalesCreated;
            this.reviewsCreated = reviewsCreated;
        }

        public long getCategories() {
            return categories;
        }

        public long getUsers() {
            return users;
        }

        public long getProducts() {
            return products;
        }

        public long getAddresses() {
            return addresses;
        }

        public long getOrders() {
            return orders;
        }

        public long getOrderItems() {
            return orderItems;
        }

        public long getPayments() {
            return payments;
        }

        public long getInventoryTransactions() {
            return inventoryTransactions;
        }

        public long getReviews() {
            return reviews;
        }

        public long getLegacySales() {
            return legacySales;
        }

        public int getUsersCreated() {
            return usersCreated;
        }

        public int getProductsCreated() {
            return productsCreated;
        }

        public int getAddressesCreated() {
            return addressesCreated;
        }

        public int getInventoryTransactionsCreated() {
            return inventoryTransactionsCreated;
        }

        public int getOrdersCreated() {
            return ordersCreated;
        }

        public int getLegacySalesCreated() {
            return legacySalesCreated;
        }

        public int getReviewsCreated() {
            return reviewsCreated;
        }

        @Override
        public String toString() {
            return "SeedResult{" +
                    "categories=" + categories +
                    ", users=" + users +
                    ", products=" + products +
                    ", addresses=" + addresses +
                    ", orders=" + orders +
                    ", orderItems=" + orderItems +
                    ", payments=" + payments +
                    ", inventoryTransactions=" + inventoryTransactions +
                    ", reviews=" + reviews +
                    ", legacySales=" + legacySales +
                    ", usersCreated=" + usersCreated +
                    ", productsCreated=" + productsCreated +
                    ", addressesCreated=" + addressesCreated +
                    ", inventoryTransactionsCreated=" + inventoryTransactionsCreated +
                    ", ordersCreated=" + ordersCreated +
                    ", legacySalesCreated=" + legacySalesCreated +
                    ", reviewsCreated=" + reviewsCreated +
                    '}';
        }
    }
}
