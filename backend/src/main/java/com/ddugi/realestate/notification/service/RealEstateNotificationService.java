package com.ddugi.realestate.notification.service;

import com.ddugi.realestate.domain.entity.PriceAnalysisResult;
import com.ddugi.realestate.domain.entity.PriceHistory;
import com.ddugi.realestate.domain.repository.PriceAnalysisResultRepository;
import com.ddugi.realestate.notification.config.TelegramProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 부동산 분석 결과 텔레그램 알림 서비스
 *
 * 알림 종류:
 * 1. 일일 종합 리포트  - 급등/급락/고변동성/추천 매물 요약
 * 2. 추천 매물 단독    - 추천 매물만 별도 발송
 * 3. 급등/급락 단독    - 기준치 초과 매물 즉시 알림
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RealEstateNotificationService {

    private final TelegramBotService             telegramBotService;
    private final NotificationMessageBuilder     messageBuilder;
    private final PriceAnalysisResultRepository  analysisResultRepository;
    private final TelegramProperties             telegramProperties;

    /**
     * 일일 종합 리포트 발송
     * AnalysisScheduler 에서 분석 완료 후 호출
     */
    public void sendDailyReport(String yearMonth) {
        if (!telegramBotService.isConfigured()) return;

        int topN = telegramProperties.getNotification().getTopCount();
        PriceHistory.TradeType trade = PriceHistory.TradeType.TRADE;

        List<PriceAnalysisResult> topRising      = analysisResultRepository.findTopRising(yearMonth, trade, topN);
        List<PriceAnalysisResult> topFalling     = analysisResultRepository.findTopFalling(yearMonth, trade, topN);
        List<PriceAnalysisResult> highVolatility = analysisResultRepository.findHighVolatility(yearMonth, trade, topN);
        List<PriceAnalysisResult> recommended    = analysisResultRepository
            .findByBaseYearMonthAndRecommendedTrueOrderByChangeRateDesc(yearMonth);

        String message = messageBuilder.buildDailyReport(
            yearMonth, topRising, topFalling, highVolatility, recommended);

        telegramBotService.sendMessage(message);
        log.info("[Notification] 일일 리포트 발송 완료 - {}", yearMonth);
    }

    /**
     * 추천 매물 단독 알림
     */
    public void sendRecommendedAlert(String yearMonth) {
        if (!telegramBotService.isConfigured()) return;

        List<PriceAnalysisResult> recommended = analysisResultRepository
            .findByBaseYearMonthAndRecommendedTrueOrderByChangeRateDesc(yearMonth);

        if (recommended.isEmpty()) {
            log.info("[Notification] 추천 매물 없음 - {} 알림 스킵", yearMonth);
            return;
        }

        String message = messageBuilder.buildRecommendedAlert(recommended, yearMonth);
        telegramBotService.sendMessage(message);
        log.info("[Notification] 추천 매물 알림 발송 - {}건", recommended.size());
    }

    /**
     * 급등 매물 알림 (기준치 이상 등락률만 필터링해서 발송)
     */
    public void sendRisingAlert(String yearMonth) {
        if (!telegramBotService.isConfigured()) return;

        double threshold = telegramProperties.getNotification().getRisingThreshold();
        int topN = telegramProperties.getNotification().getTopCount();

        List<PriceAnalysisResult> items = analysisResultRepository
            .findTopRising(yearMonth, PriceHistory.TradeType.TRADE, topN * 2)
            .stream()
            .filter(r -> r.getChangeRate() != null
                && r.getChangeRate().compareTo(BigDecimal.valueOf(threshold)) >= 0)
            .limit(topN)
            .toList();

        if (items.isEmpty()) {
            log.info("[Notification] 급등 기준({})% 초과 매물 없음 - 알림 스킵", threshold);
            return;
        }

        String message = messageBuilder.buildRisingAlert(items, yearMonth);
        telegramBotService.sendMessage(message);
        log.info("[Notification] 급등 알림 발송 - {}건", items.size());
    }

    /**
     * 급락 매물 알림 (기준치 이하 등락률만 필터링해서 발송)
     */
    public void sendFallingAlert(String yearMonth) {
        if (!telegramBotService.isConfigured()) return;

        double threshold = telegramProperties.getNotification().getFallingThreshold();
        int topN = telegramProperties.getNotification().getTopCount();

        List<PriceAnalysisResult> items = analysisResultRepository
            .findTopFalling(yearMonth, PriceHistory.TradeType.TRADE, topN * 2)
            .stream()
            .filter(r -> r.getChangeRate() != null
                && r.getChangeRate().compareTo(BigDecimal.valueOf(threshold)) <= 0)
            .limit(topN)
            .toList();

        if (items.isEmpty()) {
            log.info("[Notification] 급락 기준({})% 미만 매물 없음 - 알림 스킵", threshold);
            return;
        }

        String message = messageBuilder.buildFallingAlert(items, yearMonth);
        telegramBotService.sendMessage(message);
        log.info("[Notification] 급락 알림 발송 - {}건", items.size());
    }

    /**
     * 전체 알림 일괄 발송 (리포트 + 추천 + 급등/급락)
     */
    public void sendAll(String yearMonth) {
        sendDailyReport(yearMonth);
        sendRecommendedAlert(yearMonth);
        sendRisingAlert(yearMonth);
        sendFallingAlert(yearMonth);
    }

    private String currentYearMonth() {
        return LocalDate.now().minusMonths(1).format(DateTimeFormatter.ofPattern("yyyyMM"));
    }
}
