package com.ddugi.realestate.scraper.scheduler;

import com.ddugi.realestate.scraper.service.MolitApiScraperService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 정기 스크래핑 스케줄러
 * application.yml의 scraper.schedule.cron 으로 주기 조정
 */
@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class ScrapingScheduler {

    private final MolitApiScraperService molitApiScraperService;

    // 주요 수집 대상 지역코드 (법정동코드 5자리)
    private static final List<String> TARGET_REGIONS = List.of(
        "11110",  // 서울 종로구
        "11140",  // 서울 중구
        "11170",  // 서울 용산구
        "11200",  // 서울 성동구
        "11215",  // 서울 광진구
        "11230",  // 서울 동대문구
        "11260",  // 서울 성북구
        "11290",  // 서울 강북구
        "11305",  // 서울 도봉구
        "11320",  // 서울 노원구
        "11350",  // 서울 은평구
        "11380",  // 서울 서대문구
        "11410",  // 서울 마포구
        "11440",  // 서울 양천구
        "11470",  // 서울 강서구
        "11500",  // 서울 구로구
        "11530",  // 서울 금천구
        "11545",  // 서울 영등포구
        "11560",  // 서울 동작구
        "11590",  // 서울 관악구
        "11620",  // 서울 서초구
        "11650",  // 서울 강남구
        "11680",  // 서울 송파구
        "11710"   // 서울 강동구
    );

    /**
     * 매일 오전 6시 - 전월 실거래가 수집
     * (국토부는 익월에 데이터 확정 공개)
     */
    @Scheduled(cron = "${scraper.schedule.cron}")
    public void scheduledScraping() {
        String lastMonth = LocalDate.now().minusMonths(1)
            .format(DateTimeFormatter.ofPattern("yyyyMM"));

        log.info("=== 정기 스크래핑 시작 - 대상 년월: {} ===", lastMonth);

        int totalTrade = 0, totalRent = 0;
        for (String regionCode : TARGET_REGIONS) {
            try {
                totalTrade += molitApiScraperService.scrapeAptTrade(regionCode, lastMonth);
                totalRent += molitApiScraperService.scrapeAptRent(regionCode, lastMonth);
                Thread.sleep(300); // API 과부하 방지
            } catch (Exception e) {
                log.error("지역 {} 수집 실패: {}", regionCode, e.getMessage());
            }
        }

        log.info("=== 정기 스크래핑 완료 - 매매: {}건, 전월세: {}건 ===", totalTrade, totalRent);
    }
}
