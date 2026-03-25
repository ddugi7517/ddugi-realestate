package com.ddugi.realestate.analysis.service;

import com.ddugi.realestate.domain.entity.PriceAnalysisResult;
import com.ddugi.realestate.domain.entity.PriceAnalysisResult.RecommendReason;
import com.ddugi.realestate.domain.entity.PriceHistory;
import com.ddugi.realestate.domain.entity.PriceSnapshot;
import com.ddugi.realestate.domain.repository.PriceAnalysisResultRepository;
import com.ddugi.realestate.domain.repository.PriceSnapshotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 등락률 분석 서비스
 *
 * 분석 항목:
 * 1. 전월 대비 등락률 (changeRate)
 * 2. 6개월 변동계수 (volatilityIndex = stdDev / avg * 100)
 * 3. 추천 매물 선정
 *    - REBOUND_AFTER_DROP: 2개월 연속 하락 → 이번 달 반등
 *    - STABLE_UPTREND:     변동성 낮고 3개월 연속 상승
 *    - HIGH_TRADE_VOLUME:  거래량이 지역 평균의 2배 이상
 *    - UNDERVALUED:        지역 평균 대비 15% 이상 저렴
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PriceAnalysisService {

    private final PriceSnapshotRepository    snapshotRepository;
    private final PriceAnalysisResultRepository analysisResultRepository;

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    /**
     * 특정 지역 + 기준년월 분석 실행
     */
    @Transactional
    public int analyze(String regionCode, String baseYearMonth, PriceHistory.TradeType tradeType) {
        String prevYearMonth = getPrevYearMonth(baseYearMonth);

        List<PriceSnapshot> currentSnapshots = snapshotRepository
            .findByRegionCodeAndYearMonthAndTradeType(regionCode, baseYearMonth, tradeType);

        if (currentSnapshots.isEmpty()) {
            log.warn("[Analysis] 스냅샷 없음 - 지역: {}, 년월: {}", regionCode, baseYearMonth);
            return 0;
        }

        // 지역 평균 시세 (저평가 매물 판단 기준)
        BigDecimal regionAvgPrice = calcRegionAvg(currentSnapshots);

        // 지역 평균 거래건수 (거래량 급증 판단 기준)
        double regionAvgCount = currentSnapshots.stream()
            .mapToInt(PriceSnapshot::getTradeCount).average().orElse(1);

        int count = 0;
        for (PriceSnapshot current : currentSnapshots) {

            // 중복 체크
            if (analysisResultRepository.existsByApartNameAndRegionCodeAndExclusiveAreaAndBaseYearMonthAndTradeType(
                    current.getApartName(), regionCode, current.getExclusiveArea(), baseYearMonth, tradeType)) {
                continue;
            }

            // 전월 스냅샷 조회
            Optional<PriceSnapshot> prevOpt = snapshotRepository
                .findByApartNameAndRegionCodeAndExclusiveAreaAndYearMonthAndTradeType(
                    current.getApartName(), regionCode, current.getExclusiveArea(), prevYearMonth, tradeType);

            BigDecimal changeRate = null;
            if (prevOpt.isPresent() && prevOpt.get().getAvgPrice().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal diff = current.getAvgPrice().subtract(prevOpt.get().getAvgPrice());
                changeRate = diff.divide(prevOpt.get().getAvgPrice(), 6, RoundingMode.HALF_UP)
                    .multiply(HUNDRED)
                    .setScale(4, RoundingMode.HALF_UP);
            }

            // 6개월 변동계수 계산
            BigDecimal volatility = calcVolatility(
                current.getApartName(), current.getExclusiveArea(), tradeType, baseYearMonth);

            // 추천 매물 판단
            RecommendReason reason = judgeRecommendation(
                current, prevOpt.orElse(null), changeRate, volatility,
                regionAvgPrice, regionAvgCount, regionCode, tradeType, baseYearMonth);

            PriceAnalysisResult result = PriceAnalysisResult.builder()
                .apartName(current.getApartName())
                .regionCode(regionCode)
                .exclusiveArea(current.getExclusiveArea())
                .baseYearMonth(baseYearMonth)
                .prevYearMonth(prevOpt.isPresent() ? prevYearMonth : null)
                .tradeType(tradeType)
                .currentAvgPrice(current.getAvgPrice())
                .prevAvgPrice(prevOpt.map(PriceSnapshot::getAvgPrice).orElse(null))
                .changeRate(changeRate)
                .volatilityIndex(volatility)
                .tradeCount(current.getTradeCount())
                .recommended(reason != null)
                .recommendReason(reason)
                .build();

            analysisResultRepository.save(result);
            count++;
        }

        log.info("[Analysis] 분석 완료 - 지역: {}, 년월: {}, {}건", regionCode, baseYearMonth, count);
        return count;
    }

    /**
     * 전체 지역 일괄 분석
     */
    @Transactional
    public int analyzeAll(String baseYearMonth) {
        List<String> regions = List.of(
            "11110","11140","11170","11200","11215","11230","11260","11290",
            "11305","11320","11350","11380","11410","11440","11470","11500",
            "11530","11545","11560","11590","11620","11650","11680","11710",
            "41150","41630","41360","41281","41285","41287","41480"
        );

        int total = 0;
        for (String regionCode : regions) {
            total += analyze(regionCode, baseYearMonth, PriceHistory.TradeType.TRADE);
            total += analyze(regionCode, baseYearMonth, PriceHistory.TradeType.JEONSE);
        }
        log.info("[Analysis] 전체 분석 완료 - {} 기준, 총 {}건", baseYearMonth, total);
        return total;
    }

    // ─── 추천 매물 판단 ────────────────────────────────────────────

    private RecommendReason judgeRecommendation(
        PriceSnapshot current, PriceSnapshot prev,
        BigDecimal changeRate, BigDecimal volatility,
        BigDecimal regionAvgPrice, double regionAvgCount,
        String regionCode, PriceHistory.TradeType tradeType, String baseYearMonth
    ) {
        // 1. 하락 후 반등: 이번달 상승 & 직전 2개월 연속 하락
        if (changeRate != null && changeRate.compareTo(BigDecimal.ZERO) > 0) {
            if (isConsecutiveDrop(current.getApartName(), current.getExclusiveArea(),
                    tradeType, baseYearMonth, 2)) {
                return RecommendReason.REBOUND_AFTER_DROP;
            }
        }

        // 2. 저변동 안정 상승: 변동계수 < 5% & 3개월 연속 상승
        if (volatility != null && volatility.compareTo(BigDecimal.valueOf(5)) < 0) {
            if (isConsecutiveRise(current.getApartName(), current.getExclusiveArea(),
                    tradeType, baseYearMonth, 3)) {
                return RecommendReason.STABLE_UPTREND;
            }
        }

        // 3. 거래량 급증: 현재 거래건수 > 지역 평균의 2배
        if (current.getTradeCount() >= regionAvgCount * 2 && current.getTradeCount() >= 5) {
            return RecommendReason.HIGH_TRADE_VOLUME;
        }

        // 4. 저평가: 지역 평균 대비 15% 이상 저렴
        if (regionAvgPrice != null && regionAvgPrice.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal diff = regionAvgPrice.subtract(current.getAvgPrice());
            BigDecimal ratio = diff.divide(regionAvgPrice, 4, RoundingMode.HALF_UP)
                .multiply(HUNDRED);
            if (ratio.compareTo(BigDecimal.valueOf(15)) >= 0) {
                return RecommendReason.UNDERVALUED;
            }
        }

        return null;
    }

    // ─── 헬퍼 메서드 ────────────────────────────────────────────────

    /**
     * 6개월 변동계수 (CV = 표준편차 / 평균 * 100)
     */
    private BigDecimal calcVolatility(String apartName, Double area,
                                       PriceHistory.TradeType tradeType, String baseYearMonth) {
        String sixMonthsAgo = getMonthsBefore(baseYearMonth, 6);
        List<PriceSnapshot> snapshots = snapshotRepository.findRecentSnapshots(
            apartName, area, tradeType, sixMonthsAgo);

        if (snapshots.size() < 2) return null;

        List<BigDecimal> prices = snapshots.stream()
            .map(PriceSnapshot::getAvgPrice).toList();

        BigDecimal avg = prices.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
            .divide(BigDecimal.valueOf(prices.size()), 4, RoundingMode.HALF_UP);

        if (avg.compareTo(BigDecimal.ZERO) == 0) return null;

        BigDecimal variance = prices.stream()
            .map(p -> p.subtract(avg).pow(2))
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .divide(BigDecimal.valueOf(prices.size()), 4, RoundingMode.HALF_UP);

        BigDecimal stdDev = variance.sqrt(new java.math.MathContext(6, RoundingMode.HALF_UP));
        return stdDev.divide(avg, 6, RoundingMode.HALF_UP)
            .multiply(HUNDRED)
            .setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * N개월 연속 하락 여부 확인
     * 리스트는 최신순: [현재달, 전달, 2달전, ...]
     * 현재달(idx=0)은 이미 반등으로 확인된 상태이므로, idx=1부터(전달) 비교
     */
    private boolean isConsecutiveDrop(String apartName, Double area,
                                       PriceHistory.TradeType tradeType,
                                       String baseYearMonth, int months) {
        // 현재달 포함 months+2 개 이상 필요 (현재 + N개월 하락 + 1개 비교 기준)
        List<PriceSnapshot> snapshots = snapshotRepository.findRecentSnapshots(
            apartName, area, tradeType, getMonthsBefore(baseYearMonth, months + 1));

        if (snapshots.size() < months + 2) return false;

        // idx=0 은 현재달(리바운드), idx=1부터 연속 하락 확인
        for (int i = 1; i <= months; i++) {
            BigDecimal curr = snapshots.get(i).getAvgPrice();
            BigDecimal prev = snapshots.get(i + 1).getAvgPrice();
            if (curr.compareTo(prev) >= 0) return false; // 하락이 아닌 경우
        }
        return true;
    }

    /**
     * N개월 연속 상승 여부 확인
     */
    private boolean isConsecutiveRise(String apartName, Double area,
                                       PriceHistory.TradeType tradeType,
                                       String baseYearMonth, int months) {
        List<PriceSnapshot> snapshots = snapshotRepository.findRecentSnapshots(
            apartName, area, tradeType, getMonthsBefore(baseYearMonth, months));

        if (snapshots.size() < months) return false;

        for (int i = 0; i < months - 1; i++) {
            BigDecimal curr = snapshots.get(i).getAvgPrice();
            BigDecimal prev = snapshots.get(i + 1).getAvgPrice();
            if (curr.compareTo(prev) <= 0) return false;
        }
        return true;
    }

    private BigDecimal calcRegionAvg(List<PriceSnapshot> snapshots) {
        if (snapshots.isEmpty()) return BigDecimal.ZERO;
        BigDecimal sum = snapshots.stream()
            .map(PriceSnapshot::getAvgPrice)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(snapshots.size()), 2, RoundingMode.HALF_UP);
    }

    private String getPrevYearMonth(String yearMonth) {
        return getMonthsBefore(yearMonth, 1);
    }

    private String getMonthsBefore(String yearMonth, int months) {
        LocalDate date = LocalDate.parse(yearMonth + "01", DateTimeFormatter.ofPattern("yyyyMMdd"));
        return date.minusMonths(months).format(DateTimeFormatter.ofPattern("yyyyMM"));
    }
}
