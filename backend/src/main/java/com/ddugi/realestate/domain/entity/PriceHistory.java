package com.ddugi.realestate.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 실거래가 이력
 * 국토교통부 실거래가 API 데이터 저장
 */
@Entity
@Table(
    name = "price_history",
    indexes = {
        @Index(name = "idx_price_history_property", columnList = "property_id"),
        @Index(name = "idx_price_history_trade_date", columnList = "tradeDate"),
        @Index(name = "idx_price_history_region", columnList = "regionCode, tradeDate")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class PriceHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id")
    private Property property;

    @Column(nullable = false)
    private String regionCode;     // 법정동코드

    @Column(nullable = false)
    private String apartName;      // 아파트명

    private String dong;           // 동
    private String floor;          // 층
    private Double exclusiveArea;  // 전용면적 (㎡)

    @Column(nullable = false, precision = 15, scale = 0)
    private BigDecimal tradePrice; // 거래금액 (만원)

    @Column(nullable = false)
    private LocalDate tradeDate;   // 거래일

    @Enumerated(EnumType.STRING)
    private TradeType tradeType;   // 매매, 전세, 월세

    private BigDecimal deposit;    // 보증금 (전세/월세)
    private BigDecimal monthlyRent; // 월세

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    public enum TradeType {
        TRADE,   // 매매
        JEONSE,  // 전세
        MONTHLY  // 월세
    }
}
