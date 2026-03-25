package com.ddugi.realestate.analysis;

import com.ddugi.realestate.analysis.dto.PriceAnalysisResponse;
import com.ddugi.realestate.analysis.dto.PriceAnalysisResponse.AnalysisItem;
import com.ddugi.realestate.analysis.dto.PriceAnalysisResponse.RegionSummary;
import com.ddugi.realestate.analysis.service.PriceAnalysisService;
import com.ddugi.realestate.analysis.service.SnapshotService;
import com.ddugi.realestate.domain.entity.PriceAnalysisResult;
import com.ddugi.realestate.domain.entity.PriceHistory;
import com.ddugi.realestate.domain.repository.PriceAnalysisResultRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Tag(name = "Analysis", description = "부동산 시세 등락률 분석 API")
@RestController
@RequestMapping("/api/analysis")
@RequiredArgsConstructor
public class PriceAnalysisController {

    private final PriceAnalysisService    analysisService;
    private final SnapshotService         snapshotService;
    private final PriceAnalysisResultRepository analysisResultRepository;

    // ─── 분석 실행 ──────────────────────────────────────────────────

    @Operation(summary = "스냅샷 생성 + 분석 실행",
               description = "특정 년월의 스냅샷을 생성하고 등락률을 분석합니다. yearMonth 미입력 시 전월 기준.")
    @PostMapping("/run")
    public ResponseEntity<Map<String, Object>> runAnalysis(
        @RequestParam(required = false) String yearMonth
    ) {
        String target = yearMonth != null ? yearMonth
            : LocalDate.now().minusMonths(1).format(DateTimeFormatter.ofPattern("yyyyMM"));

        int snapshots = snapshotService.buildSnapshots(target);
        int analyzed  = analysisService.analyzeAll(target);

        return ResponseEntity.ok(Map.of(
            "yearMonth", target,
            "snapshotCount", snapshots,
            "analysisCount", analyzed
        ));
    }

    @Operation(summary = "특정 지역 분석 실행")
    @PostMapping("/run/region")
    public ResponseEntity<Map<String, Object>> runRegionAnalysis(
        @RequestParam String regionCode,
        @RequestParam String yearMonth,
        @RequestParam(defaultValue = "TRADE") String tradeType
    ) {
        PriceHistory.TradeType type = PriceHistory.TradeType.valueOf(tradeType);
        snapshotService.buildSnapshotForRegion(regionCode, yearMonth, type);
        int count = analysisService.analyze(regionCode, yearMonth, type);

        return ResponseEntity.ok(Map.of(
            "regionCode", regionCode,
            "yearMonth", yearMonth,
            "tradeType", tradeType,
            "analysisCount", count
        ));
    }

    // ─── 조회 ───────────────────────────────────────────────────────

    @Operation(summary = "지역 분석 요약",
               description = "지역별 상승/하락/보합 현황 + 급등/급락/고변동성/추천 매물 TOP5")
    @GetMapping("/summary/region/{regionCode}")
    public ResponseEntity<RegionSummary> getRegionSummary(
        @PathVariable String regionCode,
        @RequestParam(required = false) String yearMonth,
        @RequestParam(defaultValue = "TRADE") String tradeType
    ) {
        String target = resolveYearMonth(yearMonth);
        PriceHistory.TradeType type = PriceHistory.TradeType.valueOf(tradeType);

        List<PriceAnalysisResult> all = analysisResultRepository
            .findByRegionCodeAndBaseYearMonthAndTradeTypeOrderByChangeRateDesc(regionCode, target, type);

        List<AnalysisItem> topRising     = analysisResultRepository.findTopRisingByRegion(regionCode, target, type, 5)
            .stream().map(AnalysisItem::from).toList();
        List<AnalysisItem> topFalling    = analysisResultRepository.findTopFallingByRegion(regionCode, target, type, 5)
            .stream().map(AnalysisItem::from).toList();
        List<AnalysisItem> highVolatility = analysisResultRepository.findHighVolatilityByRegion(regionCode, target, type, 5)
            .stream().map(AnalysisItem::from).toList();
        List<AnalysisItem> recommended   = analysisResultRepository
            .findByRegionCodeAndBaseYearMonthAndRecommendedTrueOrderByChangeRateDesc(regionCode, target)
            .stream().map(AnalysisItem::from).toList();

        long rising  = all.stream().filter(r -> r.getChangeRate() != null && r.getChangeRate().compareTo(BigDecimal.ZERO) > 0).count();
        long falling = all.stream().filter(r -> r.getChangeRate() != null && r.getChangeRate().compareTo(BigDecimal.ZERO) < 0).count();
        long stable  = all.size() - rising - falling;

        BigDecimal avgChangeRate = all.stream()
            .filter(r -> r.getChangeRate() != null)
            .map(PriceAnalysisResult::getChangeRate)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        long withRate = all.stream().filter(r -> r.getChangeRate() != null).count();
        if (withRate > 0) {
            avgChangeRate = avgChangeRate.divide(BigDecimal.valueOf(withRate), 4, RoundingMode.HALF_UP);
        }

        RegionSummary summary = RegionSummary.builder()
            .regionCode(regionCode)
            .yearMonth(target)
            .tradeType(tradeType)
            .totalCount(all.size())
            .risingCount(rising)
            .fallingCount(falling)
            .stableCount(stable)
            .avgChangeRate(avgChangeRate)
            .topRising(topRising)
            .topFalling(topFalling)
            .highVolatility(highVolatility)
            .recommended(recommended)
            .build();

        return ResponseEntity.ok(summary);
    }

    @Operation(summary = "급등 매물 목록", description = "전월 대비 등락률 상위 N개")
    @GetMapping("/top/rising")
    public ResponseEntity<List<AnalysisItem>> getTopRising(
        @RequestParam(required = false) String yearMonth,
        @RequestParam(defaultValue = "TRADE") String tradeType,
        @RequestParam(defaultValue = "10") int limit
    ) {
        String target = resolveYearMonth(yearMonth);
        PriceHistory.TradeType type = PriceHistory.TradeType.valueOf(tradeType);
        List<AnalysisItem> result = analysisResultRepository.findTopRising(target, type, limit)
            .stream().map(AnalysisItem::from).toList();
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "급락 매물 목록", description = "전월 대비 등락률 하위 N개")
    @GetMapping("/top/falling")
    public ResponseEntity<List<AnalysisItem>> getTopFalling(
        @RequestParam(required = false) String yearMonth,
        @RequestParam(defaultValue = "TRADE") String tradeType,
        @RequestParam(defaultValue = "10") int limit
    ) {
        String target = resolveYearMonth(yearMonth);
        PriceHistory.TradeType type = PriceHistory.TradeType.valueOf(tradeType);
        List<AnalysisItem> result = analysisResultRepository.findTopFalling(target, type, limit)
            .stream().map(AnalysisItem::from).toList();
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "고변동성 매물 목록", description = "6개월 변동계수 상위 N개")
    @GetMapping("/top/volatile")
    public ResponseEntity<List<AnalysisItem>> getHighVolatility(
        @RequestParam(required = false) String yearMonth,
        @RequestParam(defaultValue = "TRADE") String tradeType,
        @RequestParam(defaultValue = "10") int limit
    ) {
        String target = resolveYearMonth(yearMonth);
        PriceHistory.TradeType type = PriceHistory.TradeType.valueOf(tradeType);
        List<AnalysisItem> result = analysisResultRepository.findHighVolatility(target, type, limit)
            .stream().map(AnalysisItem::from).toList();
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "추천 매물 목록")
    @GetMapping("/recommended")
    public ResponseEntity<List<AnalysisItem>> getRecommended(
        @RequestParam(required = false) String yearMonth
    ) {
        String target = resolveYearMonth(yearMonth);
        List<AnalysisItem> result = analysisResultRepository
            .findByBaseYearMonthAndRecommendedTrueOrderByChangeRateDesc(target)
            .stream().map(AnalysisItem::from).toList();
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "아파트 시세 추이", description = "특정 아파트의 최근 N개월 등락률 추이")
    @GetMapping("/trend")
    public ResponseEntity<List<AnalysisItem>> getTrend(
        @RequestParam String regionCode,
        @RequestParam String apartName,
        @RequestParam(defaultValue = "TRADE") String tradeType
    ) {
        PriceHistory.TradeType type = PriceHistory.TradeType.valueOf(tradeType);
        // 최근 12개월 분석 결과 조회
        List<AnalysisItem> result = analysisResultRepository
            .findByRegionCodeAndBaseYearMonthAndTradeTypeOrderByChangeRateDesc(
                regionCode,
                LocalDate.now().minusMonths(1).format(DateTimeFormatter.ofPattern("yyyyMM")),
                type)
            .stream()
            .filter(r -> r.getApartName().contains(apartName))
            .map(AnalysisItem::from)
            .toList();
        return ResponseEntity.ok(result);
    }

    private String resolveYearMonth(String yearMonth) {
        return yearMonth != null ? yearMonth
            : LocalDate.now().minusMonths(1).format(DateTimeFormatter.ofPattern("yyyyMM"));
    }
}
