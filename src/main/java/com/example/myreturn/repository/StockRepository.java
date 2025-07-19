package com.example.myreturn.repository;

import com.example.myreturn.model.Stock;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface StockRepository extends JpaRepository<Stock, Long> {
    List<Stock> findByExchange(String exchange);
} 