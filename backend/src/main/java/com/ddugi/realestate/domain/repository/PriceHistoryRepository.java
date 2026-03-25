package com.ddugi.realestate.domain.repository;

import com.ddugi.realestate.domain.entity.PriceHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface PriceHistoryRepository extends JpaRepository<PriceHistory, Long> {

    // 지역코드 + 기간으로 조회
    List<PriceHistory> findByRegionCodeAndTradeDateBetweenOrderByTradeDateDesc(
        String regionCode, LocalDate from, LocalDate to
    );

    // 아파트명 + 기간으로 조회
    List<PriceHistory> findByApartNameContainingAndTradeDateBetweenOrderByTradeDateDesc(
        String apartName, LocalDate from, LocalDate to
    );

    // 중복 체크 (같은 날짜, 아파트, 면적, 층, 금액)
    boolean existsByApartNameAndTradeDateAndExclusiveAreaAndFloorAndTradePrice(
        String apartName, LocalDate tradeDate, Double exclusiveArea, String floor,
        java.math.BigDecimal tradePrice
    );

    // 등락률 계산용: 최근 N개월 평균 시세 조회
    @Query("""
        SELECT ph.apartName, ph.exclusiveArea,
               AVG(ph.tradePrice) as avgPrice,
               MAX(ph.tradePrice) as maxPrice,
               MIN(ph.tradePrice) as minPrice,
               COUNT(ph.id) as tradeCount
        FROM PriceHistory ph
        WHERE ph.regionCode = :regionCode
          AND ph.tradeDate >= :fromDate
          AND ph.tradeType = 'TRADE'
        GROUP BY ph.apartName, ph.exclusiveArea
        ORDER BY tradeCount DESC
        """)
    List<Object[]> findPriceSummaryByRegion(
        @Param("regionCode") String regionCode,
        @Param("fromDate") LocalDate fromDate
    );

    // 스냅샷 생성용: 특정 년월의 아파트별 집계
    @Query("""
        SELECT ph.apartName, ph.regionCode, ph.exclusiveArea,
               AVG(ph.tradePrice)  as avgPrice,
               MIN(ph.tradePrice)  as minPrice,
               MAX(ph.tradePrice)  as maxPrice,
               COUNT(ph.id)        as tradeCount
        FROM PriceHistory ph
        WHERE ph.regionCode = :regionCode
          AND FUNCTION('TO_CHAR', ph.tradeDate, 'YYYYMM') = :yearMonth
          AND ph.tradeType = :tradeType
        GROUP BY ph.apartName, ph.regionCode, ph.exclusiveArea
        HAVING COUNT(ph.id) >= 1
        """)
    List<Object[]> aggregateByRegionAndYearMonth(
        @Param("regionCode") String regionCode,
        @Param("yearMonth") String yearMonth,
        @Param("tradeType") PriceHistory.TradeType tradeType
    );

    // 표준편차 계산용: 아파트 + 면적 + 기간 내 거래가 목록
    @Query("""
        SELECT ph.tradePrice
        FROM PriceHistory ph
        WHERE ph.apartName = :apartName
          AND ph.exclusiveArea = :area
          AND ph.tradeType = :tradeType
          AND ph.tradeDate >= :fromDate
        ORDER BY ph.tradeDate DESC
        """)
    List<java.math.BigDecimal> findTradePrices(
        @Param("apartName") String apartName,
        @Param("area") Double area,
        @Param("tradeType") PriceHistory.TradeType tradeType,
        @Param("fromDate") LocalDate fromDate
    );

    // property 테이블 구축용: 고유 아파트 목록 추출
    @Query(value = "SELECT apart_name, region_code, dong, COUNT(*) as trade_count FROM price_history WHERE trade_type = 'TRADE' GROUP BY apart_name, region_code, dong ORDER BY apart_name", nativeQuery = true)
    List<Object[]> findDistinctApartments();
}
