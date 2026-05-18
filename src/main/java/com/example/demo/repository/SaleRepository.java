package com.example.demo.repository;

import com.example.demo.model.Sale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SaleRepository extends JpaRepository<Sale, Long> {

    long countByProcessed(boolean processed);

    @Query("SELECT MIN(s.id) FROM Sale s WHERE s.processed = false")
    Long findMinUnprocessedId();

    @Query("SELECT MAX(s.id) FROM Sale s WHERE s.processed = false")
    Long findMaxUnprocessedId();
}
