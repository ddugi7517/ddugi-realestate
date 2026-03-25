package com.ddugi.realestate.analysis.service;

import com.ddugi.realestate.domain.entity.PriceHistory;
import com.ddugi.realestate.domain.entity.PriceSnapshot;
import com.ddugi.realestate.domain.repository.PriceHistoryRepository;
import com.ddugi.realestate.domain.repository.PriceSnapshotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 월별 시세 스냅샷 생성 서비스
 * 분석 전 PriceHistory → PriceSnapshot 집계 단계
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SnapshotService {

    private final PriceHistoryRepository priceHistoryRepository;
    private final PriceSnapshotRepository priceSnapshotRepository;

    private static final List<String> TARGET_REGIONS = List.of(
        "11110","11140","11170","11200","11215","11230","11260","11290",
        "11305","11320","11350","11380","11410","11440","11470","11500",
        "11530","11545","11560","11590","11620","11650","11680","11710",
        "41150","41630","41360","41281","41285","41287","41480"
    );

    /**
     * 특정 년월의 전체 지역 스냅샷 생성
     */
    @Transactional
    public int buildSnapshots(String yearMonth) {
        int total = 0;
        for (String regionCode : TARGET_REGIONS) {
            total += buildSnapshotForRegion(regionCode, yearMonth, PriceHistory.TradeType.TRADE);
            total += buildSnapshotForRegion(regionCode, yearMonth, PriceHistory.TradeType.JEONSE);
        }
        log.info("[Snapshot] {} 스냅샷 생성 완료 - 총 {}건", yearMonth, total);
        return total;
    }

    /**
     * 특정 지역 + 년월 + 거래유형 스냅샷 생성
     */
    @Transactional
    public int buildSnapshotForRegion(String regionCode, String yearMonth, PriceHistory.TradeType tradeType) {
        List<Object[]> aggregated = priceHistoryRepository
            .aggregateByRegionAndYearMonth(regionCode, yearMonth, tradeType);

        int count = 0;
        for (Object[] row : aggregated) {
            String apartName    = (String)  row[0];
            String region       = (String)  row[1];
            Double area         = (Double)  row[2];
            BigDecimal avgPrice = toBigDecimal(row[3]);
            BigDecimal minPrice = toBigDecimal(row[4]);
            BigDecimal maxPrice = toBigDecimal(row[5]);
            int tradeCount      = ((Number) row[6]).intValue();

            // 이미 존재하면 스킵
            if (priceSnapshotRepository.existsByApartNameAndRegionCodeAndExclusiveAreaAndYearMonthAndTradeType(
                    apartName, region, area, yearMonth, tradeType)) {
                continue;
            }

            // 표준편차 계산
            BigDecimal stdDev = calcStdDev(apartName, area, tradeType, yearMonth);

            PriceSnapshot snapshot = PriceSnapshot.builder()
                .apartName(apartName)
                .regionCode(region)
                .exclusiveArea(area)
                .yearMonth(yearMonth)
                .tradeType(tradeType)
                .avgPrice(avgPrice)
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .stdDev(stdDev)
                .tradeCount(tradeCount)
                .build();

            priceSnapshotRepository.save(snapshot);
            count++;
        }
        return count;
    }

    /**
     * 표준편차 계산 (해당 월 거래 데이터 기반)
     */
    private BigDecimal calcStdDev(String apartName, Double area,
                                   PriceHistory.TradeType tradeType, String yearMonth) {
        // 해당 월 1일부터 말일까지 거래가 목록
        LocalDate from = LocalDate.parse(yearMonth + "01", DateTimeFormatter.ofPattern("yyyyMMdd"));
        LocalDate to   = from.withDayOfMonth(from.lengthOfMonth());

        List<BigDecimal> prices = priceHistoryRepository.findTradePrices(
            apartName, area, tradeType, from
        ).stream()
            .filter(p -> p != null)
            .toList();

        if (prices.size() < 2) return BigDecimal.ZERO;

        // 평균
        BigDecimal sum = prices.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal avg = sum.divide(BigDecimal.valueOf(prices.size()), 4, RoundingMode.HALF_UP);

        // 분산
        BigDecimal variance = prices.stream()
            .map(p -> p.subtract(avg).pow(2))
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .divide(BigDecimal.valueOf(prices.size()), 4, RoundingMode.HALF_UP);

        // 표준편차
        return variance.sqrt(new MathContext(6, RoundingMode.HALF_UP));
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        return new BigDecimal(value.toString()).setScale(2, RoundingMode.HALF_UP);
    }
}
