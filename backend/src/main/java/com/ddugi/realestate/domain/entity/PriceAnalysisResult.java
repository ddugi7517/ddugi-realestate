package com.ddugi.realestate.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 등락률 분석 결과
 * 전월 대비 등락률, 변동성, 추천 여부 저장
 */
@Entity
@Table(
    name = "price_analysis_result",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_analysis_apt_area_ym",
        columnNames = {"apartName", "regionCode", "exclusiveArea", "baseYearMonth", "tradeType"}
    ),
    indexes = {
        @Index(name = "idx_analysis_region_ym",       columnList = "regionCode, baseYearMonth"),
        @Index(name = "idx_analysis_change_rate",      columnList = "changeRate"),
        @Index(name = "idx_analysis_volatility",       columnList = "volatilityIndex"),
        @Index(name = "idx_analysis_recommended",      columnList = "recommended")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class PriceAnalysisResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String apartName;           // 아파트명

    @Column(nullable = false)
    private String regionCode;          // 법정동코드

    private Double exclusiveArea;       // 전용면적 (㎡)

    @Column(nullable = false)
    private String baseYearMonth;       // 기준 년월 (YYYYMM, 이번달)

    private String prevYearMonth;       // 비교 년월 (YYYYMM, 전월)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PriceHistory.TradeType tradeType;

    // 시세
    @Column(precision = 15, scale = 2)
    private BigDecimal currentAvgPrice; // 현재월 평균가 (만원)

    @Column(precision = 15, scale = 2)
    private BigDecimal prevAvgPrice;    // 전월 평균가 (만원)

    // 등락률 (%)
    @Column(precision = 8, scale = 4)
    private BigDecimal changeRate;      // 전월 대비 등락률 (%)  ex) +5.23, -2.10

    // 변동성
    @Column(precision = 8, scale = 4)
    private BigDecimal volatilityIndex; // 변동계수 CV (%) - 최근 6개월 기준

    // 추천 관련
    private Boolean recommended;        // 추천 매물 여부

    @Enumerated(EnumType.STRING)
    private RecommendReason recommendReason; // 추천 이유

    private Integer tradeCount;         // 거래 건수 (이번달)

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    public enum RecommendReason {
        REBOUND_AFTER_DROP,     // 하락 후 반등 (저가 매수 신호)
        STABLE_UPTREND,         // 저변동 + 안정 상승
        HIGH_TRADE_VOLUME,      // 거래량 급증 (시장 관심 증가)
        UNDERVALUED             // 지역 평균 대비 저평가
    }
}
