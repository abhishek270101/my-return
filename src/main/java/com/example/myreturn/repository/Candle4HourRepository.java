package com.example.myreturn.repository;

import com.example.myreturn.model.Candle4Hour;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface Candle4HourRepository extends JpaRepository<Candle4Hour, Long> {
    
    List<Candle4Hour> findByTradingSymbolOrderByTimestampDesc(String tradingSymbol);
    
    List<Candle4Hour> findByTradingSymbolAndTimestampBetweenOrderByTimestampDesc(
            String tradingSymbol, LocalDateTime startTime, LocalDateTime endTime);
    
    @Query("SELECT c FROM Candle4Hour c WHERE c.tradingSymbol = :symbol AND c.timestamp >= :startTime ORDER BY c.timestamp DESC")
    List<Candle4Hour> findByTradingSymbolAndTimestampAfter(@Param("symbol") String symbol, @Param("startTime") LocalDateTime startTime);
} 