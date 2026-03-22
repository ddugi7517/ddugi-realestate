package com.ddugi.realestate.analysis.service;

import com.ddugi.realestate.domain.entity.PriceHistory;
import com.ddugi.realestate.domain.entity.PriceSnapshot;
import com.ddugi.realestate.domain.repository.PriceHistoryRepository;
import com.ddugi.realestate.domain.repository.PriceSnapshotRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SnapshotServiceTest {

    @Mock
    private PriceHistoryRepository priceHistoryRepository;

    @Mock
    private PriceSnapshotRepository priceSnapshotRepository;

    @InjectMocks
    private SnapshotService snapshotService;

    private static final String REGION_CODE = "11110";
    private static final String YEAR_MONTH  = "202502";
    private static final PriceHistory.TradeType TRADE = PriceHistory.TradeType.TRADE;

    /**
     * aggregateByRegionAndYearMonth 반환용 Object[] 생성 헬퍼
     * 순서: [apartName, regionCode, exclusiveArea, avgPrice, minPrice, maxPrice, tradeCount]
     */
    private Object[] aggregateRow(String apartName, String regionCode, Double area,
                                   double avgPrice, double minPrice, double maxPrice, int tradeCount) {
        return new Object[]{apartName, regionCode, area,
                BigDecimal.valueOf(avgPrice), BigDecimal.valueOf(minPrice),
                BigDecimal.valueOf(maxPrice), tradeCount};
    }

    /** Object[] 리스트를 명시적 타입으로 감싸는 헬퍼 (List.of(Object[]) 타입 추론 이슈 방지) */
    private List<Object[]> rows(Object[]... items) {
        List<Object[]> list = new ArrayList<>();
        for (Object[] item : items) list.add(item);
        return list;
    }

    // ─── buildSnapshotForRegion 테스트 ────────────────────────────────

    @Test
    @DisplayName("buildSnapshotForRegion_정상동작: aggregateByRegionAndYearMonth 결과로 PriceSnapshot 저장 확인")
    void buildSnapshotForRegion_정상동작() {
        // given
        Object[] row = aggregateRow("테스트아파트", REGION_CODE, 84.0,
                10000.0, 9500.0, 10500.0, 5);

        given(priceHistoryRepository.aggregateByRegionAndYearMonth(REGION_CODE, YEAR_MONTH, TRADE))
                .willReturn(rows(row));
        given(priceSnapshotRepository.existsByApartNameAndRegionCodeAndExclusiveAreaAndYearMonthAndTradeType(
                anyString(), anyString(), anyDouble(), anyString(), any()))
                .willReturn(false);
        // 표준편차 계산용 거래가 목록 (2개 이상이면 stdDev 계산)
        given(priceHistoryRepository.findTradePrices(anyString(), anyDouble(), any(), any(LocalDate.class)))
                .willReturn(List.of(
                        new BigDecimal("10000"),
                        new BigDecimal("10500"),
                        new BigDecimal("9500")
                ));

        // when
        int count = snapshotService.buildSnapshotForRegion(REGION_CODE, YEAR_MONTH, TRADE);

        // then
        assertThat(count).isEqualTo(1);

        ArgumentCaptor<PriceSnapshot> captor = ArgumentCaptor.forClass(PriceSnapshot.class);
        verify(priceSnapshotRepository).save(captor.capture());

        PriceSnapshot saved = captor.getValue();
        assertThat(saved.getApartName()).isEqualTo("테스트아파트");
        assertThat(saved.getRegionCode()).isEqualTo(REGION_CODE);
        assertThat(saved.getExclusiveArea()).isEqualTo(84.0);
        assertThat(saved.getYearMonth()).isEqualTo(YEAR_MONTH);
        assertThat(saved.getTradeType()).isEqualTo(TRADE);
        assertThat(saved.getAvgPrice().compareTo(new BigDecimal("10000.00"))).isEqualTo(0);
        assertThat(saved.getMinPrice().compareTo(new BigDecimal("9500.00"))).isEqualTo(0);
        assertThat(saved.getMaxPrice().compareTo(new BigDecimal("10500.00"))).isEqualTo(0);
        assertThat(saved.getTradeCount()).isEqualTo(5);
    }

    @Test
    @DisplayName("buildSnapshotForRegion_중복스킵: existsBy... true 이면 save 미호출")
    void buildSnapshotForRegion_중복스킵() {
        // given
        Object[] row = aggregateRow("이미존재아파트", REGION_CODE, 84.0,
                10000.0, 9000.0, 11000.0, 3);

        given(priceHistoryRepository.aggregateByRegionAndYearMonth(REGION_CODE, YEAR_MONTH, TRADE))
                .willReturn(rows(row));
        given(priceSnapshotRepository.existsByApartNameAndRegionCodeAndExclusiveAreaAndYearMonthAndTradeType(
                anyString(), anyString(), anyDouble(), anyString(), any()))
                .willReturn(true);  // 이미 존재

        // when
        int count = snapshotService.buildSnapshotForRegion(REGION_CODE, YEAR_MONTH, TRADE);

        // then
        assertThat(count).isEqualTo(0);
        verify(priceSnapshotRepository, never()).save(any(PriceSnapshot.class));
    }

    @Test
    @DisplayName("buildSnapshotForRegion_빈데이터_0반환: 집계 결과 없을 때 0 반환")
    void buildSnapshotForRegion_빈데이터_0반환() {
        // given
        given(priceHistoryRepository.aggregateByRegionAndYearMonth(REGION_CODE, YEAR_MONTH, TRADE))
                .willReturn(List.of());

        // when
        int count = snapshotService.buildSnapshotForRegion(REGION_CODE, YEAR_MONTH, TRADE);

        // then
        assertThat(count).isEqualTo(0);
        verify(priceSnapshotRepository, never()).save(any(PriceSnapshot.class));
    }

    @Test
    @DisplayName("buildSnapshotForRegion_여러아파트_일부중복: 중복 아닌 아파트만 저장")
    void buildSnapshotForRegion_여러아파트_일부중복() {
        // given
        Object[] row1 = aggregateRow("아파트A", REGION_CODE, 84.0, 10000.0, 9000.0, 11000.0, 3);
        Object[] row2 = aggregateRow("아파트B", REGION_CODE, 59.0, 8000.0, 7500.0, 8500.0, 2);

        given(priceHistoryRepository.aggregateByRegionAndYearMonth(REGION_CODE, YEAR_MONTH, TRADE))
                .willReturn(rows(row1, row2));

        // 아파트A는 이미 존재, 아파트B는 신규
        given(priceSnapshotRepository.existsByApartNameAndRegionCodeAndExclusiveAreaAndYearMonthAndTradeType(
                eq("아파트A"), anyString(), anyDouble(), anyString(), any()))
                .willReturn(true);
        given(priceSnapshotRepository.existsByApartNameAndRegionCodeAndExclusiveAreaAndYearMonthAndTradeType(
                eq("아파트B"), anyString(), anyDouble(), anyString(), any()))
                .willReturn(false);

        // 표준편차 계산용
        given(priceHistoryRepository.findTradePrices(anyString(), anyDouble(), any(), any(LocalDate.class)))
                .willReturn(List.of(new BigDecimal("8000"), new BigDecimal("8500")));

        // when
        int count = snapshotService.buildSnapshotForRegion(REGION_CODE, YEAR_MONTH, TRADE);

        // then
        assertThat(count).isEqualTo(1);

        ArgumentCaptor<PriceSnapshot> captor = ArgumentCaptor.forClass(PriceSnapshot.class);
        verify(priceSnapshotRepository).save(captor.capture());
        assertThat(captor.getValue().getApartName()).isEqualTo("아파트B");
    }

    @Test
    @DisplayName("buildSnapshotForRegion_거래가1개_stdDev0: 거래 1건이면 stdDev=0")
    void buildSnapshotForRegion_거래가1개_stdDev0() {
        // given
        Object[] row = aggregateRow("단일거래아파트", REGION_CODE, 84.0,
                10000.0, 10000.0, 10000.0, 1);

        given(priceHistoryRepository.aggregateByRegionAndYearMonth(REGION_CODE, YEAR_MONTH, TRADE))
                .willReturn(rows(row));
        given(priceSnapshotRepository.existsByApartNameAndRegionCodeAndExclusiveAreaAndYearMonthAndTradeType(
                anyString(), anyString(), anyDouble(), anyString(), any()))
                .willReturn(false);
        // 거래가 1건 → stdDev 계산에서 size < 2 → BigDecimal.ZERO 반환
        given(priceHistoryRepository.findTradePrices(anyString(), anyDouble(), any(), any(LocalDate.class)))
                .willReturn(List.of(new BigDecimal("10000")));

        // when
        int count = snapshotService.buildSnapshotForRegion(REGION_CODE, YEAR_MONTH, TRADE);

        // then
        assertThat(count).isEqualTo(1);

        ArgumentCaptor<PriceSnapshot> captor = ArgumentCaptor.forClass(PriceSnapshot.class);
        verify(priceSnapshotRepository).save(captor.capture());
        assertThat(captor.getValue().getStdDev().compareTo(BigDecimal.ZERO)).isEqualTo(0);
    }

    // ─── buildSnapshots 전체 테스트 ──────────────────────────────────

    @Test
    @DisplayName("buildSnapshots_전체지역호출: 24개 지역 x 2개 거래유형 = 48회 aggregateByRegionAndYearMonth 호출")
    void buildSnapshots_전체지역호출() {
        // given - 모든 지역에 대해 빈 결과 반환
        given(priceHistoryRepository.aggregateByRegionAndYearMonth(anyString(), anyString(), any()))
                .willReturn(List.of());

        // when
        int total = snapshotService.buildSnapshots(YEAR_MONTH);

        // then
        assertThat(total).isEqualTo(0);
        // 24 지역 * 2 (TRADE + JEONSE) = 48회 호출
        verify(priceHistoryRepository, org.mockito.Mockito.times(48))
                .aggregateByRegionAndYearMonth(anyString(), anyString(), any());
    }
}
