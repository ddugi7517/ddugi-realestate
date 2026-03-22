package com.ddugi.realestate.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 스크래핑 실행 로그
 */
@Entity
@Table(name = "scraping_log")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class ScrapingLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ScrapingSource source;  // 데이터 출처

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ScrapingStatus status;  // 성공/실패

    private String targetRegion;    // 수집 대상 지역
    private String targetYearMonth; // 수집 대상 년월 (YYYYMM)
    private Integer totalCount;     // 수집 건수
    private String errorMessage;    // 에러 메시지

    @Column(nullable = false)
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;

    public void complete(int count) {
        this.status = ScrapingStatus.SUCCESS;
        this.totalCount = count;
        this.finishedAt = LocalDateTime.now();
    }

    public void fail(String errorMessage) {
        this.status = ScrapingStatus.FAILED;
        this.errorMessage = errorMessage;
        this.finishedAt = LocalDateTime.now();
    }

    public enum ScrapingSource {
        MOLIT_APT_TRADE,  // 국토부 아파트 매매
        MOLIT_APT_RENT,   // 국토부 아파트 전월세
        NAVER             // 네이버 부동산
    }

    public enum ScrapingStatus {
        RUNNING, SUCCESS, FAILED
    }
}
