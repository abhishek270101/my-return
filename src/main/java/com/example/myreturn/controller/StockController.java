package com.example.myreturn.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import com.example.myreturn.service.StockService;
import com.example.myreturn.service.TwelveDataService;
import com.example.myreturn.model.Stock;
import com.example.myreturn.StockScheduler;

@RestController
@RequestMapping("/api/stocks")
public class StockController {
    @Autowired
    private StockService stockService;

    @Autowired
    private StockScheduler stockScheduler;
    
    @Autowired
    private TwelveDataService twelveDataService;

    @GetMapping("/nse")
    public List<Stock> getNseStocks() {
        return stockService.getStocksByExchange("NSE");
    }

    @GetMapping("/bse")
    public List<Stock> getBseStocks() {
        return stockService.getStocksByExchange("BSE");
    }

    @PostMapping("/refresh")
    public String refreshStocks() {
        stockScheduler.updateStocksManually();
        return "Stock data refresh triggered from Twelve Data API.";
    }
    
    @PostMapping("/refresh/nse")
    public String refreshNseStocks() {
        stockService.saveAll(twelveDataService.fetchNseStocks());
        return "NSE stock data refresh completed.";
    }
    
    @PostMapping("/refresh/bse")
    public String refreshBseStocks() {
        stockService.saveAll(twelveDataService.fetchBseStocks());
        return "BSE stock data refresh completed.";
    }
} 