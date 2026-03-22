package com.ddugi.realestate.notification.service;

import com.ddugi.realestate.domain.entity.PriceAnalysisResult;
import com.ddugi.realestate.domain.entity.PriceHistory;
import com.ddugi.realestate.domain.repository.PriceAnalysisResultRepository;
import com.ddugi.realestate.notification.config.TelegramProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RealEstateNotificationServiceTest {

    @Mock
    private TelegramBotService telegramBotService;

    @Mock
    private PriceAnalysisResultRepository analysisResultRepository;

    // NotificationMessageBuilder 는 실제 객체 사용
    private NotificationMessageBuilder messageBuilder;

    private TelegramProperties telegramProperties;

    private RealEstateNotificationService notificationService;

    private static final String YEAR_MONTH = "202502";

    @BeforeEach
    void setUp() {
        messageBuilder = new NotificationMessageBuilder();

        telegramProperties = new TelegramProperties();
        TelegramProperties.Bot bot = new TelegramProperties.Bot();
        bot.setToken("test-token");
        bot.setChatId("test-chat-id");
        telegramProperties.setBot(bot);

        TelegramProperties.Notification notification = new TelegramProperties.Notification();
        notification.setTopCount(5);
        notification.setRisingThreshold(3.0);
        notification.setFallingThreshold(-3.0);
        telegramProperties.setNotification(notification);

        notificationService = new RealEstateNotificationService(
                telegramBotService, messageBuilder, analysisResultRepository, telegramProperties
        );
    }

    // ─── sendDailyReport 테스트 ───────────────────────────────────────

    @Test
    @DisplayName("sendDailyReport_정상발송: isConfigured true 시 sendMessage 한 번 호출 확인")
    void sendDailyReport_정상발송() {
        // given
        given(telegramBotService.isConfigured()).willReturn(true);
        given(analysisResultRepository.findTopRising(anyString(), any(), anyInt()))
                .willReturn(List.of());
        given(analysisResultRepository.findTopFalling(anyString(), any(), anyInt()))
                .willReturn(List.of());
        given(analysisResultRepository.findHighVolatility(anyString(), any(), anyInt()))
                .willReturn(List.of());
        given(analysisResultRepository.findByBaseYearMonthAndRecommendedTrueOrderByChangeRateDesc(anyString()))
                .willReturn(List.of());

        // when
        notificationService.sendDailyReport(YEAR_MONTH);

        // then
        verify(telegramBotService).sendMessage(anyString());
    }

    @Test
    @DisplayName("sendDailyReport_봇미설정시_스킵: isConfigured false 이면 sendMessage 미호출")
    void sendDailyReport_봇미설정시_스킵() {
        // given
        given(telegramBotService.isConfigured()).willReturn(false);

        // when
        notificationService.sendDailyReport(YEAR_MONTH);

        // then
        verify(telegramBotService, never()).sendMessage(anyString());
        // repository 조회도 이루어지지 않아야 함
        verify(analysisResultRepository, never()).findTopRising(any(), any(), anyInt());
    }

    @Test
    @DisplayName("sendDailyReport_리포트데이터포함: 데이터가 있으면 해당 내용을 담아 sendMessage 호출")
    void sendDailyReport_리포트데이터포함() {
        // given
        PriceAnalysisResult risingItem = PriceAnalysisResult.builder()
                .apartName("급등아파트")
                .regionCode("11110")
                .exclusiveArea(84.0)
                .baseYearMonth(YEAR_MONTH)
                .tradeType(PriceHistory.TradeType.TRADE)
                .currentAvgPrice(new BigDecimal("15000"))
                .prevAvgPrice(new BigDecimal("12000"))
                .changeRate(new BigDecimal("25.00"))
                .tradeCount(5)
                .recommended(false)
                .build();

        given(telegramBotService.isConfigured()).willReturn(true);
        given(analysisResultRepository.findTopRising(anyString(), any(), anyInt()))
                .willReturn(List.of(risingItem));
        given(analysisResultRepository.findTopFalling(anyString(), any(), anyInt()))
                .willReturn(List.of());
        given(analysisResultRepository.findHighVolatility(anyString(), any(), anyInt()))
                .willReturn(List.of());
        given(analysisResultRepository.findByBaseYearMonthAndRecommendedTrueOrderByChangeRateDesc(anyString()))
                .willReturn(List.of());

        // when
        notificationService.sendDailyReport(YEAR_MONTH);

        // then
        verify(telegramBotService).sendMessage(argThat(msg -> msg.contains("급등아파트")));
    }

    // ─── sendRisingAlert 테스트 ───────────────────────────────────────

    @Test
    @DisplayName("sendRisingAlert_기준치미만_스킵: 등락률 2%인 데이터는 기준(3%) 미달로 스킵")
    void sendRisingAlert_기준치미만_스킵() {
        // given - changeRate = 2.00 < threshold 3.0
        PriceAnalysisResult belowThreshold = PriceAnalysisResult.builder()
                .apartName("소폭상승아파트")
                .regionCode("11110")
                .exclusiveArea(84.0)
                .baseYearMonth(YEAR_MONTH)
                .tradeType(PriceHistory.TradeType.TRADE)
                .currentAvgPrice(new BigDecimal("10200"))
                .prevAvgPrice(new BigDecimal("10000"))
                .changeRate(new BigDecimal("2.00"))
                .tradeCount(3)
                .recommended(false)
                .build();

        given(telegramBotService.isConfigured()).willReturn(true);
        given(analysisResultRepository.findTopRising(anyString(), any(), anyInt()))
                .willReturn(List.of(belowThreshold));

        // when
        notificationService.sendRisingAlert(YEAR_MONTH);

        // then - 기준치 미달이므로 items가 비어 sendMessage 미호출
        verify(telegramBotService, never()).sendMessage(anyString());
    }

    @Test
    @DisplayName("sendRisingAlert_기준치초과_발송: 등락률 5%인 데이터는 기준(3%) 초과로 발송")
    void sendRisingAlert_기준치초과_발송() {
        // given - changeRate = 5.00 >= threshold 3.0
        PriceAnalysisResult aboveThreshold = PriceAnalysisResult.builder()
                .apartName("급등아파트")
                .regionCode("11110")
                .exclusiveArea(84.0)
                .baseYearMonth(YEAR_MONTH)
                .tradeType(PriceHistory.TradeType.TRADE)
                .currentAvgPrice(new BigDecimal("10500"))
                .prevAvgPrice(new BigDecimal("10000"))
                .changeRate(new BigDecimal("5.00"))
                .tradeCount(3)
                .recommended(false)
                .build();

        given(telegramBotService.isConfigured()).willReturn(true);
        given(analysisResultRepository.findTopRising(anyString(), any(), anyInt()))
                .willReturn(List.of(aboveThreshold));

        // when
        notificationService.sendRisingAlert(YEAR_MONTH);

        // then - 기준치 초과이므로 sendMessage 호출
        verify(telegramBotService).sendMessage(anyString());
    }

    @Test
    @DisplayName("sendRisingAlert_정확히기준치_발송: 등락률 정확히 3.0%이면 발송 (compareTo >= 0)")
    void sendRisingAlert_정확히기준치_발송() {
        // given - changeRate = 3.00 == threshold 3.0
        PriceAnalysisResult exactThreshold = PriceAnalysisResult.builder()
                .apartName("정확기준아파트")
                .regionCode("11110")
                .exclusiveArea(84.0)
                .baseYearMonth(YEAR_MONTH)
                .tradeType(PriceHistory.TradeType.TRADE)
                .currentAvgPrice(new BigDecimal("10300"))
                .prevAvgPrice(new BigDecimal("10000"))
                .changeRate(new BigDecimal("3.00"))
                .tradeCount(3)
                .recommended(false)
                .build();

        given(telegramBotService.isConfigured()).willReturn(true);
        given(analysisResultRepository.findTopRising(anyString(), any(), anyInt()))
                .willReturn(List.of(exactThreshold));

        // when
        notificationService.sendRisingAlert(YEAR_MONTH);

        // then - 정확히 기준치이므로 발송
        verify(telegramBotService).sendMessage(anyString());
    }

    @Test
    @DisplayName("sendRisingAlert_changeRate_null_스킵: changeRate null인 데이터는 필터링되어 스킵")
    void sendRisingAlert_changeRate_null_스킵() {
        // given - changeRate null
        PriceAnalysisResult nullRate = PriceAnalysisResult.builder()
                .apartName("데이터부족아파트")
                .regionCode("11110")
                .exclusiveArea(84.0)
                .baseYearMonth(YEAR_MONTH)
                .tradeType(PriceHistory.TradeType.TRADE)
                .currentAvgPrice(new BigDecimal("10000"))
                .changeRate(null)
                .tradeCount(1)
                .recommended(false)
                .build();

        given(telegramBotService.isConfigured()).willReturn(true);
        given(analysisResultRepository.findTopRising(anyString(), any(), anyInt()))
                .willReturn(List.of(nullRate));

        // when
        notificationService.sendRisingAlert(YEAR_MONTH);

        // then - changeRate null이므로 필터링되어 sendMessage 미호출
        verify(telegramBotService, never()).sendMessage(anyString());
    }

    // ─── sendRecommendedAlert 테스트 ─────────────────────────────────

    @Test
    @DisplayName("sendRecommendedAlert_추천매물없으면_스킵: 추천 매물 없을 때 sendMessage 미호출")
    void sendRecommendedAlert_추천매물없으면_스킵() {
        // given
        given(telegramBotService.isConfigured()).willReturn(true);
        given(analysisResultRepository.findByBaseYearMonthAndRecommendedTrueOrderByChangeRateDesc(anyString()))
                .willReturn(List.of());

        // when
        notificationService.sendRecommendedAlert(YEAR_MONTH);

        // then
        verify(telegramBotService, never()).sendMessage(anyString());
    }

    @Test
    @DisplayName("sendRecommendedAlert_추천매물있으면_발송: 추천 매물 존재 시 sendMessage 호출")
    void sendRecommendedAlert_추천매물있으면_발송() {
        // given
        PriceAnalysisResult recommended = PriceAnalysisResult.builder()
                .apartName("추천아파트")
                .regionCode("11110")
                .exclusiveArea(84.0)
                .baseYearMonth(YEAR_MONTH)
                .tradeType(PriceHistory.TradeType.TRADE)
                .currentAvgPrice(new BigDecimal("8000"))
                .changeRate(new BigDecimal("5.00"))
                .tradeCount(3)
                .recommended(true)
                .recommendReason(PriceAnalysisResult.RecommendReason.UNDERVALUED)
                .build();

        given(telegramBotService.isConfigured()).willReturn(true);
        given(analysisResultRepository.findByBaseYearMonthAndRecommendedTrueOrderByChangeRateDesc(anyString()))
                .willReturn(List.of(recommended));

        // when
        notificationService.sendRecommendedAlert(YEAR_MONTH);

        // then
        verify(telegramBotService).sendMessage(anyString());
    }

    @Test
    @DisplayName("sendRecommendedAlert_봇미설정_스킵: isConfigured false 이면 repository 조회 없이 스킵")
    void sendRecommendedAlert_봇미설정_스킵() {
        // given
        given(telegramBotService.isConfigured()).willReturn(false);

        // when
        notificationService.sendRecommendedAlert(YEAR_MONTH);

        // then
        verify(telegramBotService, never()).sendMessage(anyString());
        verify(analysisResultRepository, never())
                .findByBaseYearMonthAndRecommendedTrueOrderByChangeRateDesc(anyString());
    }
}
