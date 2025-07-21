package com.example.myreturn.repository;

import com.example.myreturn.model.Candle1Min;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface Candle1MinRepository extends JpaRepository<Candle1Min, Long> {
    
    List<Candle1Min> findByTradingSymbolOrderByTimestampDesc(String tradingSymbol);
    
    List<Candle1Min> findByTradingSymbolAndTimestampBetweenOrderByTimestampDesc(
            String tradingSymbol, LocalDateTime startTime, LocalDateTime endTime);
    
    @Query("SELECT c FROM Candle1Min c WHERE c.tradingSymbol = :symbol AND c.timestamp >= :startTime ORDER BY c.timestamp DESC")
    List<Candle1Min> findByTradingSymbolAndTimestampAfter(@Param("symbol") String symbol, @Param("startTime") LocalDateTime startTime);
} 