package com.ddugi.realestate.scraper.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 국토교통부 실거래가 API 응답 파싱용 DTO
 */
@Getter
@Setter
public class MolitTradeDto {

    private String apartName;       // 아파트명
    private String dong;            // 법정동
    private String regionCode;      // 법정동 코드
    private String floor;           // 층
    private Double exclusiveArea;   // 전용면적
    private BigDecimal tradePrice;  // 거래금액 (만원)
    private LocalDate tradeDate;    // 거래일
    private Integer buildYear;      // 건축년도

    // 전월세용
    private BigDecimal deposit;     // 보증금
    private BigDecimal monthlyRent; // 월세
}
