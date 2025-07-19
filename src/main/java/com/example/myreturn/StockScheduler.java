package com.example.myreturn;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.example.myreturn.service.StockService;
import com.example.myreturn.service.TwelveDataService;

import jakarta.annotation.PostConstruct;

@Component
public class StockScheduler {
    private final TwelveDataService twelveDataService;
    private final StockService stockService;

    public StockScheduler(TwelveDataService twelveDataService, StockService stockService) {
        this.twelveDataService = twelveDataService;
        this.stockService = stockService;
    }

    // Runs every day at 2:00 AM
    @Scheduled(cron = "0 0 2 * * ?")
    public void updateStocksDaily() {
        System.out.println("Starting daily stock update from Twelve Data API...");
        
        // Fetch and save NSE stocks
        System.out.println("Fetching NSE stocks...");
        stockService.saveAll(twelveDataService.fetchNseStocks());
        
        // Fetch and save BSE stocks
        System.out.println("Fetching BSE stocks...");
        stockService.saveAll(twelveDataService.fetchBseStocks());
        
        System.out.println("Daily stock update completed.");
    }
    
    // Manual trigger method for testing
    public void updateStocksManually() {
        System.out.println("Manual stock update triggered...");
        updateStocksDaily();
    }
} 

