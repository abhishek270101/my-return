package com.example.myreturn.repository;

import com.example.myreturn.model.Candle1Day;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface Candle1DayRepository extends JpaRepository<Candle1Day, Long> {
    
    List<Candle1Day> findByTradingSymbolOrderByTimestampDesc(String tradingSymbol);
    
    List<Candle1Day> findByTradingSymbolAndTimestampBetweenOrderByTimestampDesc(
            String tradingSymbol, LocalDateTime startTime, LocalDateTime endTime);
    
    @Query("SELECT c FROM Candle1Day c WHERE c.tradingSymbol = :symbol AND c.timestamp >= :startTime ORDER BY c.timestamp DESC")
    List<Candle1Day> findByTradingSymbolAndTimestampAfter(@Param("symbol") String symbol, @Param("startTime") LocalDateTime startTime);
} 