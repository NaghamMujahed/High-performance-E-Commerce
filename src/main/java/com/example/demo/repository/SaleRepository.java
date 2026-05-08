package com.example.demo.repository;

import com.example.demo.model.Sale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SaleRepository extends JpaRepository<Sale, Long> {
    long countByProcessed(boolean processed);
    Page<Sale> findByProcessed(boolean processed, Pageable pageable);
}