package com.ddugi.realestate.notification.service;

import com.ddugi.realestate.domain.entity.PriceAnalysisResult;
import com.ddugi.realestate.domain.entity.PriceHistory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationMessageBuilderTest {

    private NotificationMessageBuilder messageBuilder;

    @BeforeEach
    void setUp() {
        messageBuilder = new NotificationMessageBuilder();
    }

    // ─── escape 테스트 ────────────────────────────────────────────────

    @Test
    @DisplayName("escape_특수문자_이스케이프_정상동작: '5.5%' → '5\\.5\\%'")
    void escape_특수문자_이스케이프_정상동작() {
        // when
        String result = messageBuilder.escape("5.5%");

        // then
        // % 는 이스케이프 대상 아님 (MarkdownV2 기준)
        // . 은 \. 로 이스케이프
        assertThat(result).isEqualTo("5\\.5%");
    }

    @Test
    @DisplayName("escape_언더스코어_이스케이프: 'hello_world' → 'hello\\_world'")
    void escape_언더스코어_이스케이프() {
        // when
        String result = messageBuilder.escape("hello_world");

        // then
        assertThat(result).isEqualTo("hello\\_world");
    }

    @Test
    @DisplayName("escape_복합_특수문자_이스케이프: '[TEST] +1.5%' → 각 특수문자 이스케이프")
    void escape_복합_특수문자_이스케이프() {
        // when
        String result = messageBuilder.escape("[TEST] +1.5%");

        // then
        assertThat(result).contains("\\[");
        assertThat(result).contains("\\]");
        assertThat(result).contains("\\+");
        assertThat(result).contains("\\.");
    }

    @Test
    @DisplayName("escape_null입력_빈문자열반환: null 입력 시 빈 문자열 반환")
    void escape_null입력_빈문자열반환() {
        // when
        String result = messageBuilder.escape(null);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("escape_괄호_이스케이프: '(서울)' → '\\(서울\\)'")
    void escape_괄호_이스케이프() {
        // when
        String result = messageBuilder.escape("(서울)");

        // then
        assertThat(result).isEqualTo("\\(서울\\)");
    }

    // ─── buildDailyReport 테스트 ─────────────────────────────────────

    @Test
    @DisplayName("buildDailyReport_정상동작: 빈 리스트로 호출 시 NPE 없이 메시지 생성")
    void buildDailyReport_정상동작() {
        // when
        String result = messageBuilder.buildDailyReport(
                "202502",
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );

        // then
        assertThat(result).isNotNull();
        assertThat(result).isNotEmpty();
        assertThat(result).contains("뚜기부동산 일일 리포트");
        assertThat(result).contains("데이터 없음");
        assertThat(result).contains("이번 달 추천 매물 없음");
    }

    @Test
    @DisplayName("buildDailyReport_년월포맷: '202502' → '2025년 02월' 포함")
    void buildDailyReport_년월포맷() {
        // when
        String result = messageBuilder.buildDailyReport(
                "202502",
                List.of(), List.of(), List.of(), List.of()
        );

        // then
        // escape 처리 후 형태: 2025년 02월 (한글과 숫자는 이스케이프 없음)
        assertThat(result).contains("2025년 02월");
    }

    @Test
    @DisplayName("buildDailyReport_급등매물포함: PriceAnalysisResult mock 데이터로 메시지 포함 확인")
    void buildDailyReport_급등매물포함() {
        // given
        PriceAnalysisResult rising = PriceAnalysisResult.builder()
                .apartName("급등아파트")
                .regionCode("11110")
                .exclusiveArea(84.0)
                .baseYearMonth("202502")
                .tradeType(PriceHistory.TradeType.TRADE)
                .currentAvgPrice(new BigDecimal("15000"))
                .prevAvgPrice(new BigDecimal("12000"))
                .changeRate(new BigDecimal("25.00"))
                .tradeCount(5)
                .recommended(false)
                .build();

        // when
        String result = messageBuilder.buildDailyReport(
                "202502",
                List.of(rising),
                List.of(),
                List.of(),
                List.of()
        );

        // then
        assertThat(result).contains("급등아파트");
        assertThat(result).contains("급등 매물 TOP 1");
        // +25.00% 이스케이프 처리 확인
        assertThat(result).contains("\\+25\\.00%");
    }

    @Test
    @DisplayName("buildDailyReport_추천매물포함: recommended 리스트 포함 시 추천 섹션에 아파트명 등장")
    void buildDailyReport_추천매물포함() {
        // given
        PriceAnalysisResult recommended = PriceAnalysisResult.builder()
                .apartName("추천아파트")
                .regionCode("11110")
                .exclusiveArea(59.0)
                .baseYearMonth("202502")
                .tradeType(PriceHistory.TradeType.TRADE)
                .currentAvgPrice(new BigDecimal("8000"))
                .changeRate(new BigDecimal("5.00"))
                .tradeCount(3)
                .recommended(true)
                .recommendReason(PriceAnalysisResult.RecommendReason.UNDERVALUED)
                .build();

        // when
        String result = messageBuilder.buildDailyReport(
                "202502",
                List.of(), List.of(), List.of(),
                List.of(recommended)
        );

        // then
        assertThat(result).contains("추천아파트");
        assertThat(result).contains("추천 매물");
        assertThat(result).contains("지역 평균 대비 저평가");
    }

    // ─── formatPrice 테스트 ───────────────────────────────────────────

    @Test
    @DisplayName("formatPrice_억단위_변환: 50000만원 → '5억' 포함 확인")
    void formatPrice_억단위_변환() {
        // given - 50000만원 = 5억
        PriceAnalysisResult result = PriceAnalysisResult.builder()
                .apartName("테스트아파트")
                .regionCode("11110")
                .exclusiveArea(84.0)
                .baseYearMonth("202502")
                .tradeType(PriceHistory.TradeType.TRADE)
                .currentAvgPrice(new BigDecimal("50000"))
                .changeRate(BigDecimal.ZERO)
                .tradeCount(3)
                .recommended(false)
                .build();

        // when
        String message = messageBuilder.buildDailyReport(
                "202502",
                List.of(result),
                List.of(), List.of(), List.of()
        );

        // then - formatPrice(50000) → "5억"
        assertThat(message).contains("5억");
    }

    @Test
    @DisplayName("formatPrice_억단위_나머지포함: 55000만원 → '5억 5000' 포함 확인")
    void formatPrice_억단위_나머지포함() {
        // given - 55000만원 = 5억 5000만
        PriceAnalysisResult result = PriceAnalysisResult.builder()
                .apartName("테스트아파트")
                .regionCode("11110")
                .exclusiveArea(84.0)
                .baseYearMonth("202502")
                .tradeType(PriceHistory.TradeType.TRADE)
                .currentAvgPrice(new BigDecimal("55000"))
                .changeRate(BigDecimal.ZERO)
                .tradeCount(3)
                .recommended(false)
                .build();

        // when
        String message = messageBuilder.buildDailyReport(
                "202502",
                List.of(result),
                List.of(), List.of(), List.of()
        );

        // then
        assertThat(message).contains("5억 5000");
    }

    @Test
    @DisplayName("formatPrice_만원단위: 9000만원 → '9000' 포함 확인")
    void formatPrice_만원단위() {
        // given - 9000만원 (1억 미만)
        PriceAnalysisResult result = PriceAnalysisResult.builder()
                .apartName("테스트아파트")
                .regionCode("11110")
                .exclusiveArea(84.0)
                .baseYearMonth("202502")
                .tradeType(PriceHistory.TradeType.TRADE)
                .currentAvgPrice(new BigDecimal("9000"))
                .changeRate(BigDecimal.ZERO)
                .tradeCount(3)
                .recommended(false)
                .build();

        // when
        String message = messageBuilder.buildDailyReport(
                "202502",
                List.of(result),
                List.of(), List.of(), List.of()
        );

        // then
        assertThat(message).contains("9000");
    }

    // ─── buildRisingAlert / buildFallingAlert 테스트 ─────────────────

    @Test
    @DisplayName("buildRisingAlert_정상동작: 급등 알림 메시지 형식 확인")
    void buildRisingAlert_정상동작() {
        // given
        PriceAnalysisResult item = PriceAnalysisResult.builder()
                .apartName("급등아파트")
                .regionCode("11110")
                .exclusiveArea(84.0)
                .baseYearMonth("202502")
                .tradeType(PriceHistory.TradeType.TRADE)
                .currentAvgPrice(new BigDecimal("12000"))
                .prevAvgPrice(new BigDecimal("10000"))
                .changeRate(new BigDecimal("20.00"))
                .tradeCount(5)
                .recommended(false)
                .build();

        // when
        String result = messageBuilder.buildRisingAlert(List.of(item), "202502");

        // then
        assertThat(result).isNotNull();
        assertThat(result).contains("급등 매물 알림");
        assertThat(result).contains("급등아파트");
        assertThat(result).contains("\\+20\\.00%");
    }

    @Test
    @DisplayName("buildFallingAlert_정상동작: 급락 알림 메시지 형식 확인")
    void buildFallingAlert_정상동작() {
        // given
        PriceAnalysisResult item = PriceAnalysisResult.builder()
                .apartName("급락아파트")
                .regionCode("11110")
                .exclusiveArea(84.0)
                .baseYearMonth("202502")
                .tradeType(PriceHistory.TradeType.TRADE)
                .currentAvgPrice(new BigDecimal("8000"))
                .prevAvgPrice(new BigDecimal("10000"))
                .changeRate(new BigDecimal("-20.00"))
                .tradeCount(2)
                .recommended(false)
                .build();

        // when
        String result = messageBuilder.buildFallingAlert(List.of(item), "202502");

        // then
        assertThat(result).isNotNull();
        assertThat(result).contains("급락 매물 알림");
        assertThat(result).contains("급락아파트");
        assertThat(result).contains("\\-20\\.00%");
    }
}
