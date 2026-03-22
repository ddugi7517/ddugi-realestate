package com.ddugi.realestate.domain.repository;

import com.ddugi.realestate.domain.entity.PriceAnalysisResult;
import com.ddugi.realestate.domain.entity.PriceHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PriceAnalysisResultRepository extends JpaRepository<PriceAnalysisResult, Long> {

    // 지역 + 기준년월 분석 결과 조회
    List<PriceAnalysisResult> findByRegionCodeAndBaseYearMonthAndTradeTypeOrderByChangeRateDesc(
        String regionCode, String baseYearMonth, PriceHistory.TradeType tradeType
    );

    // 등락률 상위 N개 (급등 매물)
    @Query("""
        SELECT r FROM PriceAnalysisResult r
        WHERE r.baseYearMonth = :yearMonth
          AND r.tradeType = :tradeType
          AND r.changeRate IS NOT NULL
        ORDER BY r.changeRate DESC
        LIMIT :limit
        """)
    List<PriceAnalysisResult> findTopRising(
        @Param("yearMonth") String yearMonth,
        @Param("tradeType") PriceHistory.TradeType tradeType,
        @Param("limit") int limit
    );

    // 등락률 하위 N개 (급락 매물)
    @Query("""
        SELECT r FROM PriceAnalysisResult r
        WHERE r.baseYearMonth = :yearMonth
          AND r.tradeType = :tradeType
          AND r.changeRate IS NOT NULL
        ORDER BY r.changeRate ASC
        LIMIT :limit
        """)
    List<PriceAnalysisResult> findTopFalling(
        @Param("yearMonth") String yearMonth,
        @Param("tradeType") PriceHistory.TradeType tradeType,
        @Param("limit") int limit
    );

    // 변동성 상위 N개 (고변동성 매물)
    @Query("""
        SELECT r FROM PriceAnalysisResult r
        WHERE r.baseYearMonth = :yearMonth
          AND r.tradeType = :tradeType
          AND r.volatilityIndex IS NOT NULL
        ORDER BY r.volatilityIndex DESC
        LIMIT :limit
        """)
    List<PriceAnalysisResult> findHighVolatility(
        @Param("yearMonth") String yearMonth,
        @Param("tradeType") PriceHistory.TradeType tradeType,
        @Param("limit") int limit
    );

    // 추천 매물
    List<PriceAnalysisResult> findByBaseYearMonthAndRecommendedTrueOrderByChangeRateDesc(
        String baseYearMonth
    );

    boolean existsByApartNameAndRegionCodeAndExclusiveAreaAndBaseYearMonthAndTradeType(
        String apartName, String regionCode, Double exclusiveArea,
        String baseYearMonth, PriceHistory.TradeType tradeType
    );
}
