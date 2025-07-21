package com.example.myreturn.service;

import com.example.myreturn.model.*;
import com.example.myreturn.repository.*;
import com.example.myreturn.util.NiftySymbols;
import com.example.myreturn.dto.CandleQueryDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

@Service
public class GrowwHistoricalDataService {
    
    @Autowired
    private Candle1MinRepository candle1MinRepository;
    
    @Autowired
    private Candle5MinRepository candle5MinRepository;
    
    @Autowired
    private Candle10MinRepository candle10MinRepository;
    
    @Autowired
    private Candle1HourRepository candle1HourRepository;
    
    @Autowired
    private Candle4HourRepository candle4HourRepository;
    
    @Autowired
    private Candle1DayRepository candle1DayRepository;
    
    @Autowired
    private Candle1WeekRepository candle1WeekRepository;
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final ExecutorService executorService;
    
    // Rate limiting for Historical Data type
    private final AtomicInteger requestsPerSecond = new AtomicInteger(0);
    private final AtomicInteger requestsPerMinute = new AtomicInteger(0);
    private final AtomicInteger requestsPerDay = new AtomicInteger(0);
    
    // Rate limit constants for Historical Data API
    private static final int MAX_REQUESTS_PER_SECOND = 3;   // Reduced for better control
    private static final int MAX_REQUESTS_PER_MINUTE = 60;  // Reduced for better control
    private static final int MAX_REQUESTS_PER_DAY = 5000;   // Increased to handle 500 symbols
    
    @Value("${groww.api.key:}")
    private String apiKey;
    
    @Value("${groww.api.secret:}")
    private String apiSecret;
    
    public GrowwHistoricalDataService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        this.objectMapper = new ObjectMapper();
        this.executorService = Executors.newFixedThreadPool(3); // Reduced thread pool for better rate limiting
        
        // Start rate limit reset schedulers
        startRateLimitSchedulers();
    }
    
    private void startRateLimitSchedulers() {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(3);
        
        // Reset per-second counter every second
        scheduler.scheduleAtFixedRate(() -> {
            requestsPerSecond.set(0);
        }, 1, 1, TimeUnit.SECONDS);
        
        // Reset per-minute counter every minute
        scheduler.scheduleAtFixedRate(() -> {
            requestsPerMinute.set(0);
        }, 1, 1, TimeUnit.MINUTES);
        
        // Reset per-day counter every day
        scheduler.scheduleAtFixedRate(() -> {
            requestsPerDay.set(0);
        }, 1, 1, TimeUnit.DAYS);
    }
    
    public void fetchAndStoreHistoricalData(List<String> symbols) {
        fetchAndStoreHistoricalData(symbols, false); // Default to all intervals
    }
    
    public void fetchAndStoreHistoricalData(List<String> symbols, boolean essentialOnly) {
        System.out.println("Starting to fetch data for " + symbols.size() + " symbols");
        System.out.println("Mode: " + (essentialOnly ? "Essential intervals only" : "All intervals"));
        
        // Process symbols in smaller batches to respect rate limits
        int batchSize = 5; // Process only 5 symbols at a time
        for (int i = 0; i < symbols.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, symbols.size());
            List<String> batch = symbols.subList(i, endIndex);
            
            System.out.println("Processing batch " + (i/batchSize + 1) + " of " + ((symbols.size() + batchSize - 1)/batchSize) + " (symbols " + (i+1) + "-" + endIndex + ")");
            
            for (String symbol : batch) {
                if (essentialOnly) {
                    // Fetch only essential intervals (1day, 1hour, 5min) - 3 API calls per symbol
                    fetchDataForIntervalWithRateLimit(symbol, 1440, "1day");
                    fetchDataForIntervalWithRateLimit(symbol, 60, "1hour");
                    fetchDataForIntervalWithRateLimit(symbol, 5, "5min");
                } else {
                    // Fetch data for all intervals with proper rate limiting
                    fetchDataForIntervalWithRateLimit(symbol, 1, "1min");
                    fetchDataForIntervalWithRateLimit(symbol, 5, "5min");
                    fetchDataForIntervalWithRateLimit(symbol, 10, "10min");
                    fetchDataForIntervalWithRateLimit(symbol, 60, "1hour");
                    fetchDataForIntervalWithRateLimit(symbol, 240, "4hour");
                    fetchDataForIntervalWithRateLimit(symbol, 1440, "1day");
                    fetchDataForIntervalWithRateLimit(symbol, 10080, "1week");
                }
            }
            
            // Add longer delay between batches to respect rate limits
            if (endIndex < symbols.size()) {
                try {
                    System.out.println("Waiting 5 seconds before next batch...");
                    Thread.sleep(5000); // 5 second delay between batches
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        
        System.out.println("Completed fetching data for all symbols");
    }
    
    public void fetchAllIntervalsForSymbol(String symbol) {
        System.out.println("🚀 Starting to fetch ALL intervals for symbol: " + symbol);
        System.out.println("📊 Intervals to fetch: 1min, 5min, 10min, 1hour, 4hour, 1day, 1week");
        
        // Fetch data for all intervals for a single symbol
        fetchDataForIntervalWithRateLimit(symbol, 1, "1min");
        fetchDataForIntervalWithRateLimit(symbol, 5, "5min");
        fetchDataForIntervalWithRateLimit(symbol, 10, "10min");
        fetchDataForIntervalWithRateLimit(symbol, 60, "1hour");
        fetchDataForIntervalWithRateLimit(symbol, 240, "4hour");
        fetchDataForIntervalWithRateLimit(symbol, 1440, "1day");
        fetchDataForIntervalWithRateLimit(symbol, 10080, "1week");
        
        System.out.println("✅ All intervals data fetching started for symbol: " + symbol);
    }
    
    public void fetchDataForInterval(String symbol, int intervalMinutes, String intervalName) {
        fetchDataForIntervalWithRateLimit(symbol, intervalMinutes, intervalName);
    }
    
    private void fetchDataForIntervalWithRateLimit(String symbol, int intervalMinutes, String intervalName) {
        CompletableFuture.runAsync(() -> {
            try {
                System.out.println("⏳ Waiting for rate limit availability for " + symbol + " at " + intervalName + " interval...");
                // Wait for rate limit availability
                waitForRateLimit();
                
                LocalDateTime endTime = LocalDateTime.now();
                LocalDateTime startTime = getStartTimeForInterval(intervalMinutes);
                
                String url = buildGrowwApiUrl(symbol, startTime, endTime, intervalMinutes);
                
                HttpHeaders headers = new HttpHeaders();
                headers.set("Accept", "application/json");
                headers.set("Authorization", "Bearer " + apiKey);
                headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
                headers.set("Content-Type", "application/json");
                
                HttpEntity<String> entity = new HttpEntity<>(headers);
                
                // Add delay between requests
                Thread.sleep(200); // 200ms delay between requests
                
                System.out.println("🌐 Making API request for " + symbol + " at " + intervalName + " interval...");
                System.out.println("📅 Date range: " + startTime + " to " + endTime);
                
                ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
                
                if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                    JsonNode rootNode = objectMapper.readTree(response.getBody());
                    
                    if ("SUCCESS".equals(rootNode.get("status").asText())) {
                        JsonNode payload = rootNode.get("payload");
                        JsonNode candles = payload.get("candles");
                        
                        System.out.println("💾 Saving " + candles.size() + " candles for " + symbol + " at " + intervalName + " interval...");
                        
                        for (JsonNode candle : candles) {
                            saveCandleData(symbol, intervalName, candle);
                        }
                        
                        System.out.println("✅ Successfully fetched and saved " + candles.size() + " candles for " + symbol + " at " + intervalName + " interval");
                    } else {
                        System.err.println("❌ API Error for " + symbol + " at " + intervalName + ": " + rootNode.get("message").asText());
                    }
                } else if (response.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                    System.err.println("⏰ Rate limit exceeded for " + symbol + " at " + intervalName + " interval. Waiting 60 seconds...");
                    Thread.sleep(60000); // Wait 1 minute
                } else if (response.getStatusCode() == HttpStatus.FORBIDDEN) {
                    System.err.println("🚫 403 Forbidden for " + symbol + " at " + intervalName + " interval. Check API key and permissions.");
                    System.err.println("Response: " + response.getBody());
                } else {
                    System.err.println("❌ HTTP Error for " + symbol + " at " + intervalName + ": " + response.getStatusCode());
                    System.err.println("Response: " + response.getBody());
                }
            } catch (Exception e) {
                System.err.println("💥 Error fetching data for " + symbol + " at " + intervalName + " interval: " + e.getMessage());
            }
        }, executorService);
    }
    
    private void waitForRateLimit() {
        while (true) {
            if (checkRateLimits()) {
                break;
            }
            try {
                System.out.println("Rate limit reached, waiting 1 second...");
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
    
    private boolean checkRateLimits() {
        // Check per-second limit
        if (requestsPerSecond.get() >= MAX_REQUESTS_PER_SECOND) {
            return false;
        }
        
        // Check per-minute limit
        if (requestsPerMinute.get() >= MAX_REQUESTS_PER_MINUTE) {
            return false;
        }
        
        // Check per-day limit
        if (requestsPerDay.get() >= MAX_REQUESTS_PER_DAY) {
            return false;
        }
        
        // Increment counters
        requestsPerSecond.incrementAndGet();
        requestsPerMinute.incrementAndGet();
        requestsPerDay.incrementAndGet();
        
        return true;
    }
    
    private LocalDateTime getStartTimeForInterval(int intervalMinutes) {
        LocalDateTime now = LocalDateTime.now();
        
        switch (intervalMinutes) {
            case 1: // 1 min - last 7 days
                return now.minusDays(7);
            case 5: // 5 min - last 15 days
                return now.minusDays(15);
            case 10: // 10 min - last 30 days
                return now.minusDays(30);
            case 60: // 1 hour - last 150 days
                return now.minusDays(150);
            case 240: // 4 hours - last 365 days
                return now.minusDays(365);
            case 1440: // 1 day - last 3 years
                return now.minusDays(1080);
            case 10080: // 1 week - full history
                return now.minusYears(5);
            default:
                return now.minusDays(30);
        }
    }
    
    private String buildGrowwApiUrl(String symbol, LocalDateTime startTime, LocalDateTime endTime, int intervalMinutes) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        
        // Use simple date format without URL encoding
        String startTimeStr = startTime.format(formatter);
        String endTimeStr = endTime.format(formatter);
        
        return String.format(
            "https://api.groww.in/v1/historical/candle/range?exchange=NSE&segment=CASH&trading_symbol=%s&start_time=%s&end_time=%s&interval_in_minutes=%d",
            symbol,
            startTimeStr,
            endTimeStr,
            intervalMinutes
        );
    }
    
    private void saveCandleData(String symbol, String intervalName, JsonNode candle) {
        try {
            long timestamp = candle.get(0).asLong();
            double open = candle.get(1).asDouble();
            double high = candle.get(2).asDouble();
            double low = candle.get(3).asDouble();
            double close = candle.get(4).asDouble();
            long volume = candle.get(5).asLong();
            
            // Convert epoch seconds to LocalDateTime using UTC timezone
            LocalDateTime candleTime = LocalDateTime.ofInstant(
                java.time.Instant.ofEpochSecond(timestamp), 
                java.time.ZoneOffset.UTC
            );
            
            switch (intervalName) {
                case "1min":
                    saveCandle1Min(symbol, candleTime, open, high, low, close, volume);
                    break;
                case "5min":
                    saveCandle5Min(symbol, candleTime, open, high, low, close, volume);
                    break;
                case "10min":
                    saveCandle10Min(symbol, candleTime, open, high, low, close, volume);
                    break;
                case "1hour":
                    saveCandle1Hour(symbol, candleTime, open, high, low, close, volume);
                    break;
                case "4hour":
                    saveCandle4Hour(symbol, candleTime, open, high, low, close, volume);
                    break;
                case "1day":
                    saveCandle1Day(symbol, candleTime, open, high, low, close, volume);
                    break;
                case "1week":
                    saveCandle1Week(symbol, candleTime, open, high, low, close, volume);
                    break;
            }
        } catch (Exception e) {
            System.err.println("Error saving candle data: " + e.getMessage());
        }
    }
    
    private void saveCandle1Min(String symbol, LocalDateTime timestamp, double open, double high, double low, double close, long volume) {
        Candle1Min candle = new Candle1Min();
        setCandleData(candle, symbol, timestamp, open, high, low, close, volume, 1);
        candle1MinRepository.save(candle);
    }
    
    private void saveCandle5Min(String symbol, LocalDateTime timestamp, double open, double high, double low, double close, long volume) {
        Candle5Min candle = new Candle5Min();
        setCandleData(candle, symbol, timestamp, open, high, low, close, volume, 5);
        candle5MinRepository.save(candle);
    }
    
    private void saveCandle10Min(String symbol, LocalDateTime timestamp, double open, double high, double low, double close, long volume) {
        Candle10Min candle = new Candle10Min();
        setCandleData(candle, symbol, timestamp, open, high, low, close, volume, 10);
        candle10MinRepository.save(candle);
    }
    
    private void saveCandle1Hour(String symbol, LocalDateTime timestamp, double open, double high, double low, double close, long volume) {
        Candle1Hour candle = new Candle1Hour();
        setCandleData(candle, symbol, timestamp, open, high, low, close, volume, 60);
        candle1HourRepository.save(candle);
    }
    
    private void saveCandle4Hour(String symbol, LocalDateTime timestamp, double open, double high, double low, double close, long volume) {
        Candle4Hour candle = new Candle4Hour();
        setCandleData(candle, symbol, timestamp, open, high, low, close, volume, 240);
        candle4HourRepository.save(candle);
    }
    
    private void saveCandle1Day(String symbol, LocalDateTime timestamp, double open, double high, double low, double close, long volume) {
        Candle1Day candle = new Candle1Day();
        setCandleData(candle, symbol, timestamp, open, high, low, close, volume, 1440);
        candle1DayRepository.save(candle);
    }
    
    private void saveCandle1Week(String symbol, LocalDateTime timestamp, double open, double high, double low, double close, long volume) {
        Candle1Week candle = new Candle1Week();
        setCandleData(candle, symbol, timestamp, open, high, low, close, volume, 10080);
        candle1WeekRepository.save(candle);
    }
    
    private void setCandleData(CandleData candle, String symbol, LocalDateTime timestamp, double open, double high, double low, double close, long volume, int intervalMinutes) {
        candle.setExchange("NSE");
        candle.setSegment("CASH");
        candle.setTradingSymbol(symbol);
        candle.setTimestamp(timestamp);
        candle.setOpen(open);
        candle.setHigh(high);
        candle.setLow(low);
        candle.setClose(close);
        candle.setVolume(volume);
        candle.setIntervalInMinutes(intervalMinutes);
    }
    
    // Get current rate limit status
    public String getRateLimitStatus() {
        return String.format("Rate Limits - Per Second: %d/%d, Per Minute: %d/%d, Per Day: %d/%d",
                requestsPerSecond.get(), MAX_REQUESTS_PER_SECOND,
                requestsPerMinute.get(), MAX_REQUESTS_PER_MINUTE,
                requestsPerDay.get(), MAX_REQUESTS_PER_DAY);
    }
    
    // Test API key validity
    public String testApiKey() {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Accept", "application/json");
            headers.set("Authorization", "Bearer " + apiKey);
            headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
            headers.set("Content-Type", "application/json");
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            // Use proper date format without URL encoding
            String testUrl = "https://api.groww.in/v1/historical/candle/range?exchange=NSE&segment=CASH&trading_symbol=WIPRO&start_time=2021-01-01 09:15:00&end_time=2021-01-01 15:15:00";
            
            ResponseEntity<String> response = restTemplate.exchange(testUrl, HttpMethod.GET, entity, String.class);
            
            return "API Test Response: " + response.getStatusCode() + " - " + response.getBody();
        } catch (Exception e) {
            return "API Test Error: " + e.getMessage();
        }
    }
    
    // Test API with key in query parameters
    public String testApiWithQueryParams() {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Accept", "application/json");
            headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
            headers.set("Content-Type", "application/json");
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            // Try with API key in query parameters
            String testUrl = "https://api.groww.in/v1/historical/candle/range?exchange=NSE&segment=CASH&trading_symbol=RELIANCE&start_time=2024-07-19%2009:15:00&end_time=2024-07-19%2015:15:00&interval_in_minutes=5&api_key=" + apiKey + "&api_secret=" + apiSecret;
            
            ResponseEntity<String> response = restTemplate.exchange(testUrl, HttpMethod.GET, entity, String.class);
            
            return "Query Param Test - Status: " + response.getStatusCode() + " - Body: " + response.getBody();
        } catch (Exception e) {
            return "Query Param Test Error: " + e.getMessage();
        }
    }
    
    // Getter methods for testing
    public String getApiKey() {
        return apiKey;
    }
    
    public String getApiSecret() {
        return apiSecret;
    }
    
    // Database Fetch Methods
    public Map<String, Object> getSymbolDataFromDB(String symbol, String interval) {
        Map<String, Object> response = new HashMap<>();
        try {
            List<?> records = new ArrayList<>();
            long count = 0;
            
            switch (interval) {
                case "1min":
                    records = candle1MinRepository.findByTradingSymbolOrderByTimestampDesc(symbol);
                    count = records.size();
                    break;
                case "5min":
                    records = candle5MinRepository.findByTradingSymbolOrderByTimestampDesc(symbol);
                    count = records.size();
                    break;
                case "10min":
                    records = candle10MinRepository.findByTradingSymbolOrderByTimestampDesc(symbol);
                    count = records.size();
                    break;
                case "1hour":
                    records = candle1HourRepository.findByTradingSymbolOrderByTimestampDesc(symbol);
                    count = records.size();
                    break;
                case "4hour":
                    records = candle4HourRepository.findByTradingSymbolOrderByTimestampDesc(symbol);
                    count = records.size();
                    break;
                case "1day":
                    records = candle1DayRepository.findByTradingSymbolOrderByTimestampDesc(symbol);
                    count = records.size();
                    break;
                case "1week":
                    records = candle1WeekRepository.findByTradingSymbolOrderByTimestampDesc(symbol);
                    count = records.size();
                    break;
                default:
                    response.put("error", "Invalid interval: " + interval);
                    return response;
            }
            
            // Ensure records are sorted by timestamp (newest first)
            // The repository method should already do this, but let's double-check
            if (records.size() > 1) {
                // Sort by timestamp in descending order if not already sorted
                records.sort((a, b) -> {
                    try {
                        java.lang.reflect.Method getTimestamp = a.getClass().getMethod("getTimestamp");
                        java.lang.reflect.Method getTimestamp2 = b.getClass().getMethod("getTimestamp");
                        LocalDateTime timestamp1 = (LocalDateTime) getTimestamp.invoke(a);
                        LocalDateTime timestamp2 = (LocalDateTime) getTimestamp2.invoke(b);
                        return timestamp2.compareTo(timestamp1); // Descending order
                    } catch (Exception e) {
                        return 0;
                    }
                });
            }
            
            response.put("symbol", symbol);
            response.put("interval", interval);
            response.put("count", count);
            response.put("records", records);
            response.put("success", true);
            
        } catch (Exception e) {
            response.put("error", "Error fetching data: " + e.getMessage());
            response.put("success", false);
        }
        return response;
    }
    
    public Map<String, Object> getSymbolAllIntervalsFromDB(String symbol) {
        Map<String, Object> response = new HashMap<>();
        try {
            Map<String, Object> intervals = new HashMap<>();
            
            List<?> records1min = candle1MinRepository.findByTradingSymbolOrderByTimestampDesc(symbol);
            List<?> records5min = candle5MinRepository.findByTradingSymbolOrderByTimestampDesc(symbol);
            List<?> records10min = candle10MinRepository.findByTradingSymbolOrderByTimestampDesc(symbol);
            List<?> records1hour = candle1HourRepository.findByTradingSymbolOrderByTimestampDesc(symbol);
            List<?> records4hour = candle4HourRepository.findByTradingSymbolOrderByTimestampDesc(symbol);
            List<?> records1day = candle1DayRepository.findByTradingSymbolOrderByTimestampDesc(symbol);
            List<?> records1week = candle1WeekRepository.findByTradingSymbolOrderByTimestampDesc(symbol);
            
            intervals.put("1min", Map.of("count", records1min.size(), "records", records1min));
            intervals.put("5min", Map.of("count", records5min.size(), "records", records5min));
            intervals.put("10min", Map.of("count", records10min.size(), "records", records10min));
            intervals.put("1hour", Map.of("count", records1hour.size(), "records", records1hour));
            intervals.put("4hour", Map.of("count", records4hour.size(), "records", records4hour));
            intervals.put("1day", Map.of("count", records1day.size(), "records", records1day));
            intervals.put("1week", Map.of("count", records1week.size(), "records", records1week));
            
            response.put("symbol", symbol);
            response.put("intervals", intervals);
            response.put("success", true);
            
        } catch (Exception e) {
            response.put("error", "Error fetching all intervals: " + e.getMessage());
            response.put("success", false);
        }
        return response;
    }
    
    public String getDatabaseStats() {
        try {
            long total1min = candle1MinRepository.count();
            long total5min = candle5MinRepository.count();
            long total10min = candle10MinRepository.count();
            long total1hour = candle1HourRepository.count();
            long total4hour = candle4HourRepository.count();
            long total1day = candle1DayRepository.count();
            long total1week = candle1WeekRepository.count();
            
            long totalRecords = total1min + total5min + total10min + total1hour + total4hour + total1day + total1week;
            
            return String.format("Database Stats - Total Records: %d (1min: %d, 5min: %d, 10min: %d, 1hour: %d, 4hour: %d, 1day: %d, 1week: %d)", 
                totalRecords, total1min, total5min, total10min, total1hour, total4hour, total1day, total1week);
        } catch (Exception e) {
            return "Error fetching database stats: " + e.getMessage();
        }
    }
    
    public String getNiftyDataFromDB(String niftyType, String interval) {
        try {
            List<String> symbols = new ArrayList<>();
            switch (niftyType) {
                case "nifty50":
                    // Use first 50 symbols from Nifty 500
                    symbols = NiftySymbols.getNifty500().subList(0, 50);
                    break;
                case "nifty100":
                    // Use first 100 symbols from Nifty 500
                    symbols = NiftySymbols.getNifty500().subList(0, 100);
                    break;
                case "nifty500":
                    symbols = NiftySymbols.getNifty500();
                    break;
                default:
                    return "Invalid nifty type: " + niftyType;
            }
            
            long totalRecords = 0;
            switch (interval) {
                case "1min":
                    totalRecords = candle1MinRepository.count();
                    break;
                case "5min":
                    totalRecords = candle5MinRepository.count();
                    break;
                case "10min":
                    totalRecords = candle10MinRepository.count();
                    break;
                case "1hour":
                    totalRecords = candle1HourRepository.count();
                    break;
                case "4hour":
                    totalRecords = candle4HourRepository.count();
                    break;
                case "1day":
                    totalRecords = candle1DayRepository.count();
                    break;
                case "1week":
                    totalRecords = candle1WeekRepository.count();
                    break;
            }
            
            return String.format("Fetched %d records for %s symbols at %s interval", totalRecords, symbols.size(), interval);
        } catch (Exception e) {
            return "Error fetching nifty data: " + e.getMessage();
        }
    }

    public Map<String, Object> getSymbolLatestDataFromDB(String symbol, String interval, int limit) {
        Map<String, Object> response = new HashMap<>();
        try {
            List<?> records = new ArrayList<>();
            long totalCount = 0;
            
            switch (interval) {
                case "1min":
                    records = candle1MinRepository.findByTradingSymbolOrderByTimestampDesc(symbol);
                    totalCount = records.size();
                    if (limit > 0 && records.size() > limit) {
                        records = records.subList(0, limit);
                    }
                    break;
                case "5min":
                    records = candle5MinRepository.findByTradingSymbolOrderByTimestampDesc(symbol);
                    totalCount = records.size();
                    if (limit > 0 && records.size() > limit) {
                        records = records.subList(0, limit);
                    }
                    break;
                case "10min":
                    records = candle10MinRepository.findByTradingSymbolOrderByTimestampDesc(symbol);
                    totalCount = records.size();
                    if (limit > 0 && records.size() > limit) {
                        records = records.subList(0, limit);
                    }
                    break;
                case "1hour":
                    records = candle1HourRepository.findByTradingSymbolOrderByTimestampDesc(symbol);
                    totalCount = records.size();
                    if (limit > 0 && records.size() > limit) {
                        records = records.subList(0, limit);
                    }
                    break;
                case "4hour":
                    records = candle4HourRepository.findByTradingSymbolOrderByTimestampDesc(symbol);
                    totalCount = records.size();
                    if (limit > 0 && records.size() > limit) {
                        records = records.subList(0, limit);
                    }
                    break;
                case "1day":
                    records = candle1DayRepository.findByTradingSymbolOrderByTimestampDesc(symbol);
                    totalCount = records.size();
                    if (limit > 0 && records.size() > limit) {
                        records = records.subList(0, limit);
                    }
                    break;
                case "1week":
                    records = candle1WeekRepository.findByTradingSymbolOrderByTimestampDesc(symbol);
                    totalCount = records.size();
                    if (limit > 0 && records.size() > limit) {
                        records = records.subList(0, limit);
                    }
                    break;
                default:
                    response.put("error", "Invalid interval: " + interval);
                    return response;
            }
            
            response.put("symbol", symbol);
            response.put("interval", interval);
            response.put("totalCount", totalCount);
            response.put("returnedCount", records.size());
            response.put("records", records);
            response.put("success", true);
            
        } catch (Exception e) {
            response.put("error", "Error fetching data: " + e.getMessage());
            response.put("success", false);
        }
        return response;
    }

    // Check Groww API usage and remaining limits
    public Map<String, Object> checkGrowwApiUsage() {
        Map<String, Object> response = new HashMap<>();
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Accept", "application/json");
            headers.set("Authorization", "Bearer " + apiKey);
            headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
            headers.set("Content-Type", "application/json");
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            // Make a test API call to check usage
            String testUrl = "https://api.groww.in/v1/historical/candle/range?exchange=NSE&segment=CASH&trading_symbol=RELIANCE&start_time=2024-01-01 09:15:00&end_time=2024-01-01 15:15:00&interval_in_minutes=1440";
            
            System.out.println("🔍 Checking Groww API usage and limits...");
            ResponseEntity<String> apiResponse = restTemplate.exchange(testUrl, HttpMethod.GET, entity, String.class);
            
            // Check response headers for rate limit info
            HttpHeaders responseHeaders = apiResponse.getHeaders();
            
            response.put("status", "success");
            response.put("apiResponseCode", apiResponse.getStatusCode().value());
            response.put("message", "API call successful - checking for rate limit headers");
            
            // Check for common rate limit headers
            String rateLimitRemaining = responseHeaders.getFirst("X-RateLimit-Remaining");
            String rateLimitLimit = responseHeaders.getFirst("X-RateLimit-Limit");
            String rateLimitReset = responseHeaders.getFirst("X-RateLimit-Reset");
            
            if (rateLimitRemaining != null) {
                response.put("remainingRequests", rateLimitRemaining);
                response.put("totalLimit", rateLimitLimit);
                response.put("resetTime", rateLimitReset);
            } else {
                response.put("remainingRequests", "Not provided by API");
                response.put("totalLimit", "Not provided by API");
                response.put("resetTime", "Not provided by API");
            }
            
            // Add all response headers for debugging
            Map<String, String> allHeaders = new HashMap<>();
            responseHeaders.forEach((key, values) -> {
                if (!values.isEmpty()) {
                    allHeaders.put(key, values.get(0));
                }
            });
            response.put("allHeaders", allHeaders);
            
            // Parse response body if available
            if (apiResponse.getBody() != null) {
                try {
                    JsonNode rootNode = objectMapper.readTree(apiResponse.getBody());
                    response.put("apiResponse", rootNode);
                } catch (Exception e) {
                    response.put("apiResponse", "Could not parse response body");
                }
            }
            
        } catch (Exception e) {
            response.put("status", "error");
            response.put("error", "Error checking API usage: " + e.getMessage());
            response.put("apiResponseCode", "N/A");
        }
        
        return response;
    }

    public List<Map<String, Object>> queryCandles(CandleQueryDTO dto) {
        int interval = dto.getInterval();
        if (interval <= 0 && dto.getDuration() > 0) {
            // Map duration (in minutes) to interval (in seconds)
            switch (dto.getDuration()) {
                case 1: interval = 60; break;
                case 5: interval = 300; break;
                case 10: interval = 600; break;
                case 60: interval = 3600; break;
                case 240: interval = 14400; break;
                case 1440: interval = 86400; break;
                case 10080: interval = 604800; break;
                default: interval = 86400; // default to 1day
            }
        }
        return queryCandles(
            dto.getSymbol(),
            interval,
            dto.getStartDate(),
            dto.getEndDate(),
            dto.getSortingOrder() == null ? "asc" : dto.getSortingOrder(),
            dto.getValues()
        );
    }

    public List<Map<String, Object>> queryCandles(String symbol, int interval, LocalDateTime start, LocalDateTime end, String sortingOrder, List<String> values) {
        List<Map<String, Object>> result = new ArrayList<>();
        try {
            List<?> candles = new ArrayList<>();
            // Select the correct repository based on interval in seconds
            switch (interval) {
                case 60:
                    candles = candle1MinRepository.findByTradingSymbolAndTimestampBetweenOrderByTimestampDesc(symbol, start, end);
                    break;
                case 300:
                    candles = candle5MinRepository.findByTradingSymbolAndTimestampBetweenOrderByTimestampDesc(symbol, start, end);
                    break;
                case 600:
                    candles = candle10MinRepository.findByTradingSymbolAndTimestampBetweenOrderByTimestampDesc(symbol, start, end);
                    break;
                case 3600:
                    candles = candle1HourRepository.findByTradingSymbolAndTimestampBetweenOrderByTimestampDesc(symbol, start, end);
                    break;
                case 14400:
                    candles = candle4HourRepository.findByTradingSymbolAndTimestampBetweenOrderByTimestampDesc(symbol, start, end);
                    break;
                case 86400:
                    candles = candle1DayRepository.findByTradingSymbolAndTimestampBetweenOrderByTimestampDesc(symbol, start, end);
                    break;
                case 604800:
                    candles = candle1WeekRepository.findByTradingSymbolAndTimestampBetweenOrderByTimestampDesc(symbol, start, end);
                    break;
                default:
                    return result;
            }
            // Sort if needed
            if (sortingOrder.equalsIgnoreCase("asc")) {
                candles.sort((a, b) -> {
                    try {
                        LocalDateTime t1 = (LocalDateTime) a.getClass().getMethod("getTimestamp").invoke(a);
                        LocalDateTime t2 = (LocalDateTime) b.getClass().getMethod("getTimestamp").invoke(b);
                        return t1.compareTo(t2);
                    } catch (Exception e) { return 0; }
                });
            } else {
                candles.sort((a, b) -> {
                    try {
                        LocalDateTime t1 = (LocalDateTime) a.getClass().getMethod("getTimestamp").invoke(a);
                        LocalDateTime t2 = (LocalDateTime) b.getClass().getMethod("getTimestamp").invoke(b);
                        return t2.compareTo(t1);
                    } catch (Exception e) { return 0; }
                });
            }
            // Map to requested fields
            for (Object candle : candles) {
                Map<String, Object> map = new HashMap<>();
                if (values == null || values.isEmpty()) {
                    // Return all fields
                    for (java.lang.reflect.Method m : candle.getClass().getMethods()) {
                        if (m.getName().startsWith("get") && m.getParameterCount() == 0) {
                            try {
                                String field = m.getName().substring(3);
                                field = Character.toLowerCase(field.charAt(0)) + field.substring(1);
                                map.put(field, m.invoke(candle));
                            } catch (Exception ignored) {}
                        }
                    }
                } else {
                    for (String field : values) {
                        try {
                            String getter = "get" + field.substring(0, 1).toUpperCase() + field.substring(1);
                            Object value = candle.getClass().getMethod(getter).invoke(candle);
                            map.put(field, value);
                        } catch (Exception ignored) {}
                    }
                }
                result.add(map);
            }
        } catch (Exception e) {
            // Optionally log error
        }
        return result;
    }
} 