package com.example.myreturn.service;

import com.example.myreturn.model.Stock;
import com.example.myreturn.repository.StockRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class StockService {
    private final StockRepository stockRepository;

    public StockService(StockRepository stockRepository) {
        this.stockRepository = stockRepository;
    }

    public List<Stock> getStocksByExchange(String exchange) {
        return stockRepository.findByExchange(exchange);
    }

    @Transactional
    public void saveAll(List<Stock> stocks) {
        stockRepository.saveAll(stocks);
    }
} 