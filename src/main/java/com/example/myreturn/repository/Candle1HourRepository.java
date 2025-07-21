package com.example.myreturn.repository;

import com.example.myreturn.model.Candle1Hour;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface Candle1HourRepository extends JpaRepository<Candle1Hour, Long> {
    
    List<Candle1Hour> findByTradingSymbolOrderByTimestampDesc(String tradingSymbol);
    
    List<Candle1Hour> findByTradingSymbolAndTimestampBetweenOrderByTimestampDesc(
            String tradingSymbol, LocalDateTime startTime, LocalDateTime endTime);
    
    @Query("SELECT c FROM Candle1Hour c WHERE c.tradingSymbol = :symbol AND c.timestamp >= :startTime ORDER BY c.timestamp DESC")
    List<Candle1Hour> findByTradingSymbolAndTimestampAfter(@Param("symbol") String symbol, @Param("startTime") LocalDateTime startTime);
} 