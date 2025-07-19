package com.example.myreturn.service;

import com.example.myreturn.model.Stock;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;

@Service
public class TwelveDataService {
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    @Value("${twelvedata.api.key:}")
    private String apiKey;
    
    public TwelveDataService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        this.objectMapper = new ObjectMapper();
    }
    
    public List<Stock> fetchNseStocks() {
        return fetchStocksFromAPI("NSE");
    }
    
    public List<Stock> fetchBseStocks() {
        return fetchStocksFromAPI("BSE");
    }
    
    private List<Stock> fetchStocksFromAPI(String exchange) {
        List<Stock> stocks = new ArrayList<>();
        
        try {
            String url = String.format("https://api.twelvedata.com/stocks?exchange=%s&apikey=%s", exchange, apiKey);
            System.out.println("Fetching stocks from Twelve Data API for exchange: " + exchange);
            
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode rootNode = objectMapper.readTree(response.getBody());
                
                if (rootNode.has("data") && rootNode.get("data").isArray()) {
                    JsonNode dataArray = rootNode.get("data");
                    
                    for (JsonNode stockNode : dataArray) {
                        Stock stock = new Stock();
                        
                        stock.setSymbol(getStringValue(stockNode, "symbol"));
                        stock.setName(getStringValue(stockNode, "name"));
                        stock.setExchange(getStringValue(stockNode, "exchange"));
                        stock.setCurrency(getStringValue(stockNode, "currency"));
                        stock.setType(getStringValue(stockNode, "type"));
                        stock.setCountry(getStringValue(stockNode, "country"));
                        stock.setMicCode(getStringValue(stockNode, "mic_code"));
                        stock.setFigiCode(getStringValue(stockNode, "figi_code"));
                        stock.setCfiCode(getStringValue(stockNode, "cfi_code"));
                        stock.setIsin(getStringValue(stockNode, "isin"));
                        stock.setCusip(getStringValue(stockNode, "cusip"));
                        
                        stocks.add(stock);
                    }
                }
                
                System.out.println("Successfully fetched " + stocks.size() + " stocks from " + exchange);
            } else {
                System.err.println("Failed to fetch data from Twelve Data API for " + exchange + ". Status: " + response.getStatusCode());
            }
            
        } catch (Exception e) {
            System.err.println("Error fetching stocks from Twelve Data API for " + exchange + ": " + e.getMessage());
            e.printStackTrace();
        }
        
        return stocks;
    }
    
    private String getStringValue(JsonNode node, String fieldName) {
        return node.has(fieldName) && !node.get(fieldName).isNull() ? node.get(fieldName).asText() : null;
    }
} 