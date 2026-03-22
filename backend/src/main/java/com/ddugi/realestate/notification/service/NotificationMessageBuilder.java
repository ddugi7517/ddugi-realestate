package com.ddugi.realestate.notification.service;

import com.ddugi.realestate.domain.entity.PriceAnalysisResult;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 텔레그램 알림 메시지 포맷터
 * MarkdownV2 형식 사용 (특수문자 이스케이프 필요)
 */
@Component
public class NotificationMessageBuilder {

    private static final DateTimeFormatter YM_FMT = DateTimeFormatter.ofPattern("yyyy년 MM월");

    // ─── 일일 리포트 ──────────────────────────────────────────────

    /**
     * 매일 오전 분석 완료 후 발송하는 일일 종합 리포트
     */
    public String buildDailyReport(
        String yearMonth,
        List<PriceAnalysisResult> topRising,
        List<PriceAnalysisResult> topFalling,
        List<PriceAnalysisResult> highVolatility,
        List<PriceAnalysisResult> recommended
    ) {
        String ym = formatYearMonth(yearMonth);
        StringBuilder sb = new StringBuilder();

        sb.append("🏠 *뚜기부동산 일일 리포트*\n");
        sb.append("📅 ").append(escape(ym)).append(" 기준\n");
        sb.append(escape("─".repeat(28))).append("\n\n");

        // 급등 TOP
        sb.append("🔺 *급등 매물 TOP ").append(topRising.size()).append("*\n");
        if (topRising.isEmpty()) {
            sb.append("  데이터 없음\n");
        } else {
            topRising.forEach(r -> sb.append(formatItem(r, true)));
        }
        sb.append("\n");

        // 급락 TOP
        sb.append("🔻 *급락 매물 TOP ").append(topFalling.size()).append("*\n");
        if (topFalling.isEmpty()) {
            sb.append("  데이터 없음\n");
        } else {
            topFalling.forEach(r -> sb.append(formatItem(r, true)));
        }
        sb.append("\n");

        // 고변동성
        sb.append("⚡ *고변동성 매물 TOP ").append(highVolatility.size()).append("*\n");
        if (highVolatility.isEmpty()) {
            sb.append("  데이터 없음\n");
        } else {
            highVolatility.forEach(r -> sb.append(formatVolatileItem(r)));
        }
        sb.append("\n");

        // 추천 매물
        sb.append("⭐ *추천 매물 \\(").append(recommended.size()).append("건\\)*\n");
        if (recommended.isEmpty()) {
            sb.append("  이번 달 추천 매물 없음\n");
        } else {
            recommended.forEach(r -> sb.append(formatRecommendedItem(r)));
        }

        sb.append("\n").append(escape("─".repeat(28))).append("\n");
        sb.append("_").append(escape(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))).append(" 자동 발송_");

        return sb.toString();
    }

    // ─── 추천 매물 단독 알림 ─────────────────────────────────────

    public String buildRecommendedAlert(List<PriceAnalysisResult> items, String yearMonth) {
        String ym = formatYearMonth(yearMonth);
        StringBuilder sb = new StringBuilder();

        sb.append("⭐ *추천 매물 알림*\n");
        sb.append("📅 ").append(escape(ym)).append("\n");
        sb.append(escape("─".repeat(28))).append("\n\n");

        items.forEach(r -> {
            sb.append(formatRecommendedItem(r));
            sb.append("\n");
        });

        return sb.toString();
    }

    // ─── 급등/급락 단독 알림 ─────────────────────────────────────

    public String buildRisingAlert(List<PriceAnalysisResult> items, String yearMonth) {
        return buildChangeAlert("🔺 급등 매물 알림", items, yearMonth);
    }

    public String buildFallingAlert(List<PriceAnalysisResult> items, String yearMonth) {
        return buildChangeAlert("🔻 급락 매물 알림", items, yearMonth);
    }

    private String buildChangeAlert(String title, List<PriceAnalysisResult> items, String yearMonth) {
        StringBuilder sb = new StringBuilder();
        sb.append("*").append(escape(title)).append("*\n");
        sb.append("📅 ").append(escape(formatYearMonth(yearMonth))).append("\n");
        sb.append(escape("─".repeat(28))).append("\n\n");
        items.forEach(r -> sb.append(formatItem(r, false)).append("\n"));
        return sb.toString();
    }

    // ─── 아이템 포맷 ─────────────────────────────────────────────

    private String formatItem(PriceAnalysisResult r, boolean compact) {
        StringBuilder sb = new StringBuilder();
        String arrow = getChangeArrow(r.getChangeRate());
        String rate  = formatRate(r.getChangeRate());

        sb.append("  ").append(arrow).append(" *").append(escape(r.getApartName())).append("*");

        if (r.getExclusiveArea() != null) {
            double pyeong = Math.round(r.getExclusiveArea() / 3.306 * 10.0) / 10.0;
            sb.append(" \\(").append(escape(String.valueOf(pyeong))).append("평\\)");
        }
        sb.append("\n");

        sb.append("     💰 ").append(escape(formatPrice(r.getCurrentAvgPrice()))).append("만원");
        if (r.getPrevAvgPrice() != null) {
            sb.append(" ").append(escape("→")).append(" ").append(escape(rate));
        }
        sb.append("\n");

        if (!compact && r.getTradeCount() != null) {
            sb.append("     📊 거래량: ").append(r.getTradeCount()).append("건\n");
        }
        return sb.toString();
    }

    private String formatVolatileItem(PriceAnalysisResult r) {
        StringBuilder sb = new StringBuilder();
        String vi = r.getVolatilityIndex() != null
            ? r.getVolatilityIndex().setScale(1, RoundingMode.HALF_UP) + "%" : "\\-";

        sb.append("  ⚡ *").append(escape(r.getApartName())).append("*");
        if (r.getExclusiveArea() != null) {
            double pyeong = Math.round(r.getExclusiveArea() / 3.306 * 10.0) / 10.0;
            sb.append(" \\(").append(escape(String.valueOf(pyeong))).append("평\\)");
        }
        sb.append("\n");
        sb.append("     변동성: ").append(escape(vi))
          .append("  등락: ").append(escape(formatRate(r.getChangeRate()))).append("\n");
        return sb.toString();
    }

    private String formatRecommendedItem(PriceAnalysisResult r) {
        StringBuilder sb = new StringBuilder();
        String badge = getRecommendBadge(r.getRecommendReason());
        String reasonKo = getRecommendReasonKo(r.getRecommendReason());

        sb.append("  ").append(badge).append(" *").append(escape(r.getApartName())).append("*");
        if (r.getExclusiveArea() != null) {
            double pyeong = Math.round(r.getExclusiveArea() / 3.306 * 10.0) / 10.0;
            sb.append(" \\(").append(escape(String.valueOf(pyeong))).append("평\\)");
        }
        sb.append("\n");
        sb.append("     💰 ").append(escape(formatPrice(r.getCurrentAvgPrice()))).append("만원");
        sb.append("  ").append(escape(formatRate(r.getChangeRate()))).append("\n");
        sb.append("     📌 ").append(escape(reasonKo)).append("\n");
        return sb.toString();
    }

    // ─── 유틸 ────────────────────────────────────────────────────

    private String getChangeArrow(BigDecimal rate) {
        if (rate == null) return "➖";
        int cmp = rate.compareTo(BigDecimal.ZERO);
        return cmp > 0 ? "🔺" : cmp < 0 ? "🔻" : "➖";
    }

    private String formatRate(BigDecimal rate) {
        if (rate == null) return "-";
        String prefix = rate.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "";
        return prefix + rate.setScale(2, RoundingMode.HALF_UP) + "%";
    }

    private String formatPrice(BigDecimal price) {
        if (price == null) return "-";
        long val = price.longValue();
        if (val >= 10000) {
            long uk = val / 10000;
            long man = val % 10000;
            return man == 0 ? uk + "억" : uk + "억 " + man;
        }
        return String.valueOf(val);
    }

    private String getRecommendBadge(PriceAnalysisResult.RecommendReason reason) {
        if (reason == null) return "⭐";
        return switch (reason) {
            case REBOUND_AFTER_DROP  -> "📈";
            case STABLE_UPTREND      -> "🛡️";
            case HIGH_TRADE_VOLUME   -> "🔥";
            case UNDERVALUED         -> "💎";
        };
    }

    private String getRecommendReasonKo(PriceAnalysisResult.RecommendReason reason) {
        if (reason == null) return "";
        return switch (reason) {
            case REBOUND_AFTER_DROP  -> "하락 후 반등 - 저가 매수 신호";
            case STABLE_UPTREND      -> "저변동 안정 상승";
            case HIGH_TRADE_VOLUME   -> "거래량 급증 - 시장 관심 증가";
            case UNDERVALUED         -> "지역 평균 대비 저평가";
        };
    }

    private String formatYearMonth(String yearMonth) {
        try {
            LocalDate d = LocalDate.parse(yearMonth + "01", DateTimeFormatter.ofPattern("yyyyMMdd"));
            return d.format(YM_FMT);
        } catch (Exception e) {
            return yearMonth;
        }
    }

    /**
     * MarkdownV2 특수문자 이스케이프
     * 이스케이프 대상: _ * [ ] ( ) ~ ` > # + - = | { } . !
     */
    public String escape(String text) {
        if (text == null) return "";
        return text
            .replace("\\", "\\\\")
            .replace("_", "\\_")
            .replace("*", "\\*")
            .replace("[", "\\[")
            .replace("]", "\\]")
            .replace("(", "\\(")
            .replace(")", "\\)")
            .replace("~", "\\~")
            .replace("`", "\\`")
            .replace(">", "\\>")
            .replace("#", "\\#")
            .replace("+", "\\+")
            .replace("-", "\\-")
            .replace("=", "\\=")
            .replace("|", "\\|")
            .replace("{", "\\{")
            .replace("}", "\\}")
            .replace(".", "\\.")
            .replace("!", "\\!");
    }
}
