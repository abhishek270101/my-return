package com.example.myreturn.repository;

import com.example.myreturn.model.Candle10Min;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface Candle10MinRepository extends JpaRepository<Candle10Min, Long> {
    
    List<Candle10Min> findByTradingSymbolOrderByTimestampDesc(String tradingSymbol);
    
    List<Candle10Min> findByTradingSymbolAndTimestampBetweenOrderByTimestampDesc(
            String tradingSymbol, LocalDateTime startTime, LocalDateTime endTime);
    
    @Query("SELECT c FROM Candle10Min c WHERE c.tradingSymbol = :symbol AND c.timestamp >= :startTime ORDER BY c.timestamp DESC")
    List<Candle10Min> findByTradingSymbolAndTimestampAfter(@Param("symbol") String symbol, @Param("startTime") LocalDateTime startTime);
} 