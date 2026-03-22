package com.ddugi.realestate.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 월별 아파트 평균 시세 스냅샷
 * 매월 집계 후 저장해두어 등락률 계산에 활용
 */
@Entity
@Table(
    name = "price_snapshot",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_snapshot_apt_area_ym",
        columnNames = {"apartName", "regionCode", "exclusiveArea", "yearMonth", "tradeType"}
    ),
    indexes = {
        @Index(name = "idx_snapshot_region_ym", columnList = "regionCode, yearMonth"),
        @Index(name = "idx_snapshot_apart_ym",  columnList = "apartName, yearMonth")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class PriceSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String apartName;       // 아파트명

    @Column(nullable = false)
    private String regionCode;      // 법정동코드

    private Double exclusiveArea;   // 전용면적 (㎡)

    @Column(nullable = false)
    private String yearMonth;       // 년월 (YYYYMM)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PriceHistory.TradeType tradeType;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal avgPrice;    // 평균 거래가 (만원)

    @Column(precision = 15, scale = 2)
    private BigDecimal minPrice;    // 최저 거래가

    @Column(precision = 15, scale = 2)
    private BigDecimal maxPrice;    // 최고 거래가

    @Column(precision = 10, scale = 4)
    private BigDecimal stdDev;      // 표준편차 (변동성 계산용)

    @Column(nullable = false)
    private Integer tradeCount;     // 거래 건수

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
