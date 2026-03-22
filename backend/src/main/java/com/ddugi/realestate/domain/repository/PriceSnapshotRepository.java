package com.ddugi.realestate.domain.repository;

import com.ddugi.realestate.domain.entity.PriceHistory;
import com.ddugi.realestate.domain.entity.PriceSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PriceSnapshotRepository extends JpaRepository<PriceSnapshot, Long> {

    Optional<PriceSnapshot> findByApartNameAndRegionCodeAndExclusiveAreaAndYearMonthAndTradeType(
        String apartName, String regionCode, Double exclusiveArea,
        String yearMonth, PriceHistory.TradeType tradeType
    );

    // 특정 지역 + 년월 스냅샷 전체 조회
    List<PriceSnapshot> findByRegionCodeAndYearMonthAndTradeType(
        String regionCode, String yearMonth, PriceHistory.TradeType tradeType
    );

    // 아파트 + 면적의 최근 N개월 스냅샷 (변동성 계산용)
    @Query("""
        SELECT ps FROM PriceSnapshot ps
        WHERE ps.apartName = :apartName
          AND ps.exclusiveArea = :area
          AND ps.tradeType = :tradeType
          AND ps.yearMonth >= :fromYearMonth
        ORDER BY ps.yearMonth DESC
        """)
    List<PriceSnapshot> findRecentSnapshots(
        @Param("apartName") String apartName,
        @Param("area") Double area,
        @Param("tradeType") PriceHistory.TradeType tradeType,
        @Param("fromYearMonth") String fromYearMonth
    );

    boolean existsByApartNameAndRegionCodeAndExclusiveAreaAndYearMonthAndTradeType(
        String apartName, String regionCode, Double exclusiveArea,
        String yearMonth, PriceHistory.TradeType tradeType
    );
}
