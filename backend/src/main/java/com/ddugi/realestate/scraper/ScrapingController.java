package com.ddugi.realestate.scraper;

import com.ddugi.realestate.scraper.service.MolitApiScraperService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 스크래핑 수동 실행 API
 * 개발/테스트 시 특정 지역/기간 데이터 즉시 수집
 */
@Tag(name = "Scraping", description = "실거래가 데이터 수집 API")
@RestController
@RequestMapping("/api/scraping")
@RequiredArgsConstructor
public class ScrapingController {

    private final MolitApiScraperService molitApiScraperService;

    @Operation(summary = "아파트 매매 실거래가 수집",
               description = "지역코드(법정동코드 5자리)와 년월(YYYYMM)을 지정해 수동 수집")
    @PostMapping("/molit/trade")
    public ResponseEntity<Map<String, Object>> scrapeAptTrade(
        @RequestParam String regionCode,
        @RequestParam String yearMonth
    ) {
        int count = molitApiScraperService.scrapeAptTrade(regionCode, yearMonth);
        return ResponseEntity.ok(Map.of(
            "status", "success",
            "regionCode", regionCode,
            "yearMonth", yearMonth,
            "savedCount", count
        ));
    }

    @Operation(summary = "아파트 전월세 실거래가 수집")
    @PostMapping("/molit/rent")
    public ResponseEntity<Map<String, Object>> scrapeAptRent(
        @RequestParam String regionCode,
        @RequestParam String yearMonth
    ) {
        int count = molitApiScraperService.scrapeAptRent(regionCode, yearMonth);
        return ResponseEntity.ok(Map.of(
            "status", "success",
            "regionCode", regionCode,
            "yearMonth", yearMonth,
            "savedCount", count
        ));
    }
}
