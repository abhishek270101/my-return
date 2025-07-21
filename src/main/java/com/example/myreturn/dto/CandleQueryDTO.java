package com.example.myreturn.dto;

import java.time.LocalDateTime;
import java.util.List;

public class CandleQueryDTO {
    private String symbol;
    private int interval; // in seconds
    private int duration; // in minutes
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String sortingOrder;
    private List<String> values;

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }

    public int getInterval() { return interval; }
    public void setInterval(int interval) { this.interval = interval; }

    public int getDuration() { return duration; }
    public void setDuration(int duration) { this.duration = duration; }

    public LocalDateTime getStartDate() { return startDate; }
    public void setStartDate(LocalDateTime startDate) { this.startDate = startDate; }

    public LocalDateTime getEndDate() { return endDate; }
    public void setEndDate(LocalDateTime endDate) { this.endDate = endDate; }

    public String getSortingOrder() { return sortingOrder; }
    public void setSortingOrder(String sortingOrder) { this.sortingOrder = sortingOrder; }

    public List<String> getValues() { return values; }
    public void setValues(List<String> values) { this.values = values; }
} 