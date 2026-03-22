package com.ddugi.realestate.analysis.dto;

import com.ddugi.realestate.domain.entity.PriceAnalysisResult;
import com.ddugi.realestate.domain.entity.PriceHistory;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

public class PriceAnalysisResponse {

    @Getter
    @Builder
    public static class AnalysisItem {
        private String apartName;
        private String regionCode;
        private Double exclusiveArea;        // 전용면적 ㎡
        private Double exclusiveAreaPyeong;  // 전용면적 평
        private String tradeType;
        private BigDecimal currentAvgPrice;  // 현재 평균 (만원)
        private BigDecimal prevAvgPrice;     // 전월 평균 (만원)
        private BigDecimal changeRate;       // 등락률 (%)
        private BigDecimal volatilityIndex;  // 변동성 지수 (%)
        private Integer tradeCount;
        private Boolean recommended;
        private String recommendReason;

        public static AnalysisItem from(PriceAnalysisResult result) {
            return AnalysisItem.builder()
                .apartName(result.getApartName())
                .regionCode(result.getRegionCode())
                .exclusiveArea(result.getExclusiveArea())
                .exclusiveAreaPyeong(result.getExclusiveArea() != null
                    ? Math.round(result.getExclusiveArea() / 3.306 * 10.0) / 10.0 : null)
                .tradeType(result.getTradeType().name())
                .currentAvgPrice(result.getCurrentAvgPrice())
                .prevAvgPrice(result.getPrevAvgPrice())
                .changeRate(result.getChangeRate())
                .volatilityIndex(result.getVolatilityIndex())
                .tradeCount(result.getTradeCount())
                .recommended(result.getRecommended())
                .recommendReason(result.getRecommendReason() != null
                    ? result.getRecommendReason().name() : null)
                .build();
        }
    }

    @Getter
    @Builder
    public static class RegionSummary {
        private String regionCode;
        private String yearMonth;
        private String tradeType;
        private int totalCount;              // 분석 대상 단지 수
        private long risingCount;            // 상승 단지 수
        private long fallingCount;           // 하락 단지 수
        private long stableCount;            // 보합 단지 수
        private BigDecimal avgChangeRate;    // 평균 등락률
        private List<AnalysisItem> topRising;    // 급등 TOP 5
        private List<AnalysisItem> topFalling;   // 급락 TOP 5
        private List<AnalysisItem> highVolatility; // 고변동성 TOP 5
        private List<AnalysisItem> recommended;    // 추천 매물
    }
}
