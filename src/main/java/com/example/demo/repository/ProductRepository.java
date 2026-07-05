package com.example.demo.repository;

import com.example.demo.model.OrderStatus;
import com.example.demo.model.Product;
import com.example.demo.model.ProductStatus;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findByIdWithLock(@Param("id") Long id);

    @Query("""
                select oi.product from OrderItem oi
                join oi.order o where o.status = :status
                group by oi.product
                order by sum(oi.quantity) desc
            """)
    List<Product> findTopSellingProducts(@Param("status") OrderStatus status, Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
                update Product p
                   set p.quantity = p.quantity - :quantity
                 where p.id = :productId
                   and p.active = ProductStatus.ACTIVE
                   and p.quantity >= :quantity
            """)
    int decreaseStockIfAvailable(
            @Param("productId") Long productId,
            @Param("quantity") int quantity);

    @Query("""
                select p
                from Product p
                where p.id = :productId
                  and p.active = ProductStatus.ACTIVE
            """)
    Optional<Product> findActiveProductById(
            @Param("productId") Long productId);
}