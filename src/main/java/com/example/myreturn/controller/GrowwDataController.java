package com.example.myreturn.controller;

import com.example.myreturn.dto.CandleQueryDTO;
import com.example.myreturn.service.GrowwHistoricalDataService;
import com.example.myreturn.util.NiftySymbols;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/groww")
public class GrowwDataController {
    
    @Autowired
    private GrowwHistoricalDataService growwHistoricalDataService;
    
    @PostMapping("/fetch-historical")
    public String fetchHistoricalData(@RequestBody List<String> symbols) {
        growwHistoricalDataService.fetchAndStoreHistoricalData(symbols);
        return "Historical data fetching started for " + symbols.size() + " symbols";
    }
    
    @PostMapping("/fetch-symbol")
    public String fetchHistoricalDataForSymbol(@RequestParam String symbol) {
        growwHistoricalDataService.fetchAndStoreHistoricalData(List.of(symbol));
        return "Historical data fetching started for symbol: " + symbol;
    }
    
    
    @PostMapping("/fetch-nifty500")
    public String fetchNifty500Data() {
        List<String> nifty500Symbols = NiftySymbols.getNifty500();
        growwHistoricalDataService.fetchAndStoreHistoricalData(nifty500Symbols);
        return "Historical data fetching started for Nifty 500 symbols (" + nifty500Symbols.size() + " symbols)";
    }
    
    @PostMapping("/fetch-nifty500-essential")
    public String fetchNifty500DataEssential() {
        List<String> nifty500Symbols = NiftySymbols.getNifty500();
        growwHistoricalDataService.fetchAndStoreHistoricalData(nifty500Symbols, true);
        return "Essential historical data fetching started for Nifty 500 symbols (" + nifty500Symbols.size() + " symbols) - 1day, 1hour, 5min intervals only";
    }
    
    @PostMapping("/fetch-all-intervals")
    public String fetchAllIntervalsForSymbol(@RequestParam String symbol) {
        // Fetch data for all intervals for a single symbol
        growwHistoricalDataService.fetchDataForInterval(symbol, 1, "1min");
        growwHistoricalDataService.fetchDataForInterval(symbol, 5, "5min");
        growwHistoricalDataService.fetchDataForInterval(symbol, 10, "10min");
        growwHistoricalDataService.fetchDataForInterval(symbol, 60, "1hour");
        growwHistoricalDataService.fetchDataForInterval(symbol, 240, "4hour");
        growwHistoricalDataService.fetchDataForInterval(symbol, 1440, "1day");
        growwHistoricalDataService.fetchDataForInterval(symbol, 10080, "1week");
        
        return "All intervals data fetching started for symbol: " + symbol;
    }
    
    @PostMapping("/fetch-interval")
    public String fetchSpecificInterval(@RequestParam String symbol, @RequestParam int intervalMinutes) {
        String intervalName = getIntervalName(intervalMinutes);
        growwHistoricalDataService.fetchDataForInterval(symbol, intervalMinutes, intervalName);
        return "Data fetching started for " + symbol + " at " + intervalName + " interval";
    }
    
    @PostMapping("/test-single-symbol")
    public String testSingleSymbol() {
        // Test with RELIANCE symbol only
        List<String> testSymbols = List.of("RELIANCE");
        growwHistoricalDataService.fetchAndStoreHistoricalData(testSymbols, true); // Essential intervals only
        return "Test started for RELIANCE symbol with essential intervals (1day, 1hour, 5min)";
    }
    
    @PostMapping("/test-single-symbol-all-intervals")
    public String testSingleSymbolAllIntervals() {
        // Test with RELIANCE symbol only - all intervals
        List<String> testSymbols = List.of("RELIANCE");
        growwHistoricalDataService.fetchAndStoreHistoricalData(testSymbols, false); // All intervals
        return "Test started for RELIANCE symbol with all intervals (1min, 5min, 10min, 1hour, 4hour, 1day, 1week)";
    }
    
    @GetMapping("/rate-limit-status")
    public String getRateLimitStatus() {
        return growwHistoricalDataService.getRateLimitStatus();
    }
    
    @GetMapping("/symbols-info")
    public String getSymbolsInfo() {
        return String.format("Nifty Index Information - , Nifty 500: %d symbols",
                 NiftySymbols.getNifty500Count());
    }

    @GetMapping("/test-api")
    public String testApiConfiguration() {
        return "API Configuration Test - Key: " + (growwHistoricalDataService.getApiKey() != null ? "Present" : "Missing") + 
               ", Secret: " + (growwHistoricalDataService.getApiSecret() != null ? "Present" : "Missing");
    }

    @GetMapping("/test-auth")
    public String testDifferentAuthMethods() {
        return "Testing different authentication methods for Groww API. Check logs for results.";
    }

    @GetMapping("/test-api-key")
    public String testApiKey() {
        return growwHistoricalDataService.testApiKey();
    }

    @GetMapping("/test-api-query")
    public String testApiWithQueryParams() {
        return growwHistoricalDataService.testApiWithQueryParams();
    }
    
    @GetMapping("/get-stored-data")
    public String getStoredData(@RequestParam String symbol, @RequestParam(defaultValue = "1day") String interval) {
        return "Fetching stored data for " + symbol + " at " + interval + " interval from database";
    }
    
    @GetMapping("/get-symbol-summary")
    public String getSymbolSummary(@RequestParam String symbol) {
        return "Fetching summary for " + symbol + " from all database tables";
    }
    
    @GetMapping("/get-latest-data")
    public String getLatestData(@RequestParam String symbol) {
        return "Fetching latest candle data for " + symbol + " from database";
    }
    
    @GetMapping("/get-data-count")
    public String getDataCount() {
        return "Fetching total count of stored candle data from all database tables";
    }
    
    // Database Fetch APIs
    @GetMapping("/db/symbol/{symbol}")
    public ResponseEntity<Map<String, Object>> getSymbolData(@PathVariable String symbol, @RequestParam(defaultValue = "1day") String interval) {
        Map<String, Object> serviceResponse = growwHistoricalDataService.getSymbolDataFromDB(symbol, interval);
        serviceResponse.put("timestamp", LocalDateTime.now());
        return ResponseEntity.ok(serviceResponse);
    }
    
    @GetMapping("/db/symbol/{symbol}/all-intervals")
    public ResponseEntity<Map<String, Object>> getSymbolAllIntervals(@PathVariable String symbol) {
        Map<String, Object> serviceResponse = growwHistoricalDataService.getSymbolAllIntervalsFromDB(symbol);
        serviceResponse.put("timestamp", LocalDateTime.now());
        return ResponseEntity.ok(serviceResponse);
    }
    
    @GetMapping("/db/symbol/{symbol}/latest")
    public ResponseEntity<Map<String, Object>> getSymbolLatestData(@PathVariable String symbol,
                                                                 @RequestParam(defaultValue = "1day") String interval,
                                                                 @RequestParam(defaultValue = "10") int limit) {
        Map<String, Object> serviceResponse = growwHistoricalDataService.getSymbolLatestDataFromDB(symbol, interval, limit);
        serviceResponse.put("timestamp", LocalDateTime.now());
        return ResponseEntity.ok(serviceResponse);
    }
    
    @GetMapping("/db/symbols")
    public ResponseEntity<Map<String, Object>> getAllSymbolsData(@RequestParam(defaultValue = "1day") String interval, 
                                   @RequestParam(defaultValue = "10") int limit) {
        Map<String, Object> response = new HashMap<>();
        response.put("interval", interval);
        response.put("limit", limit);
        response.put("message", "Fetching data for all symbols from database for " + interval + " interval (limit: " + limit + ")");
        response.put("timestamp", LocalDateTime.now());
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/db/symbols/list")
    public ResponseEntity<Map<String, Object>> getSymbolsListData(@RequestBody List<String> symbols, 
                                    @RequestParam(defaultValue = "1day") String interval) {
        Map<String, Object> response = new HashMap<>();
        response.put("symbols", symbols);
        response.put("interval", interval);
        response.put("message", "Fetching data for " + symbols.size() + " symbols from database for " + interval + " interval");
        response.put("timestamp", LocalDateTime.now());
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/db/nifty50")
    public ResponseEntity<Map<String, Object>> getNifty50Data(@RequestParam(defaultValue = "1day") String interval) {
        Map<String, Object> response = new HashMap<>();
        response.put("niftyType", "nifty50");
        response.put("interval", interval);
        response.put("message", growwHistoricalDataService.getNiftyDataFromDB("nifty50", interval));
        response.put("timestamp", LocalDateTime.now());
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/db/nifty100")
    public ResponseEntity<Map<String, Object>> getNifty100Data(@RequestParam(defaultValue = "1day") String interval) {
        Map<String, Object> response = new HashMap<>();
        response.put("niftyType", "nifty100");
        response.put("interval", interval);
        response.put("message", growwHistoricalDataService.getNiftyDataFromDB("nifty100", interval));
        response.put("timestamp", LocalDateTime.now());
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/db/nifty500")
    public ResponseEntity<Map<String, Object>> getNifty500Data(@RequestParam(defaultValue = "1day") String interval) {
        Map<String, Object> response = new HashMap<>();
        response.put("niftyType", "nifty500");
        response.put("interval", interval);
        response.put("message", growwHistoricalDataService.getNiftyDataFromDB("nifty500", interval));
        response.put("timestamp", LocalDateTime.now());
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/db/stats")
    public ResponseEntity<Map<String, Object>> getDatabaseStats() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", growwHistoricalDataService.getDatabaseStats());
        response.put("timestamp", LocalDateTime.now());
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/db/search")
    public ResponseEntity<Map<String, Object>> searchSymbols(@RequestParam String query, @RequestParam(defaultValue = "1day") String interval) {
        Map<String, Object> response = new HashMap<>();
        response.put("query", query);
        response.put("interval", interval);
        response.put("message", "Searching symbols containing '" + query + "' in database for " + interval + " interval");
        response.put("timestamp", LocalDateTime.now());
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/check-api-usage")
    public ResponseEntity<Map<String, Object>> checkGrowwApiUsage() {
        Map<String, Object> serviceResponse = growwHistoricalDataService.checkGrowwApiUsage();
        serviceResponse.put("timestamp", LocalDateTime.now());
        return ResponseEntity.ok(serviceResponse);
    }
    
    @PostMapping("/candles/query")
    public ResponseEntity<List<Map<String, Object>>> queryCandles(@RequestBody CandleQueryDTO dto) {
        List<Map<String, Object>> result = growwHistoricalDataService.queryCandles(dto);
        return ResponseEntity.ok(result);
    }
    
    private String getIntervalName(int intervalMinutes) {
        switch (intervalMinutes) {
            case 1: return "1min";
            case 5: return "5min";
            case 10: return "10min";
            case 60: return "1hour";
            case 240: return "4hour";
            case 1440: return "1day";
            case 10080: return "1week";
            default: return intervalMinutes + "min";
        }
    }
} 