package com.ddugi.realestate.analysis.scheduler;

import com.ddugi.realestate.analysis.service.PriceAnalysisService;
import com.ddugi.realestate.analysis.service.SnapshotService;
import com.ddugi.realestate.notification.service.RealEstateNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 스크래핑 완료 후 자동으로 분석 및 텔레그램 알림 실행
 * 스크래핑(06:00) → 분석(08:00) → 알림(08:30) 순서로 실행
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AnalysisScheduler {

    private final SnapshotService                snapshotService;
    private final PriceAnalysisService           analysisService;
    private final RealEstateNotificationService  notificationService;

    /**
     * 매일 오전 8시 - 스냅샷 생성 + 등락률 분석
     */
    @Scheduled(cron = "0 0 8 * * *")
    public void scheduledAnalysis() {
        String yearMonth = LocalDate.now().minusMonths(1)
            .format(DateTimeFormatter.ofPattern("yyyyMM"));

        log.info("=== 정기 분석 시작 - 기준 년월: {} ===", yearMonth);

        int snapshots = snapshotService.buildSnapshots(yearMonth);
        log.info("[Analysis] 스냅샷 생성: {}건", snapshots);

        int analyzed = analysisService.analyzeAll(yearMonth);
        log.info("=== 정기 분석 완료 - 분석 결과: {}건 ===", analyzed);
    }

    /**
     * 매일 오전 8시 30분 - 분석 완료 후 텔레그램 알림 발송
     */
    @Scheduled(cron = "0 30 8 * * *")
    public void scheduledNotification() {
        String yearMonth = LocalDate.now().minusMonths(1)
            .format(DateTimeFormatter.ofPattern("yyyyMM"));

        log.info("=== 텔레그램 알림 발송 시작 - {} ===", yearMonth);
        notificationService.sendAll(yearMonth);
        log.info("=== 텔레그램 알림 발송 완료 ===");
    }
}
