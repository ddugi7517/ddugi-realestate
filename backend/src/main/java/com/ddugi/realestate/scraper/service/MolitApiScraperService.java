package com.ddugi.realestate.scraper.service;

import com.ddugi.realestate.common.config.ScraperConfig.MolitApiProperties;
import com.ddugi.realestate.domain.entity.PriceHistory;
import com.ddugi.realestate.domain.entity.ScrapingLog;
import com.ddugi.realestate.domain.repository.PriceHistoryRepository;
import com.ddugi.realestate.domain.repository.ScrapingLogRepository;
import com.ddugi.realestate.scraper.dto.MolitTradeDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 국토교통부 실거래가 공공API 스크래퍼
 * API 문서: https://www.data.go.kr/data/15057511/openapi.do (아파트매매 실거래가)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MolitApiScraperService {

    private final WebClient molitWebClient;
    private final MolitApiProperties molitApiProperties;
    private final PriceHistoryRepository priceHistoryRepository;
    private final ScrapingLogRepository scrapingLogRepository;

    /**
     * 아파트 매매 실거래가 수집
     * @param regionCode 법정동코드 5자리 (예: 11110 - 서울 종로구)
     * @param yearMonth  조회 년월 (예: 202403)
     */
    @Transactional
    public int scrapeAptTrade(String regionCode, String yearMonth) {
        ScrapingLog scrapingLog = createLog(ScrapingLog.ScrapingSource.MOLIT_APT_TRADE, regionCode, yearMonth);

        try {
            String xmlResponse = callMolitApi(molitApiProperties.getAptTradeUrl(), regionCode, yearMonth);
            List<MolitTradeDto> dtos = parseTradeXml(xmlResponse, regionCode);

            int savedCount = saveTradeData(dtos, PriceHistory.TradeType.TRADE);

            scrapingLog.complete(savedCount);
            scrapingLogRepository.save(scrapingLog);
            log.info("[MOLIT] 매매 수집 완료 - 지역: {}, 년월: {}, 저장: {}건", regionCode, yearMonth, savedCount);
            return savedCount;

        } catch (Exception e) {
            scrapingLog.fail(e.getMessage());
            scrapingLogRepository.save(scrapingLog);
            log.error("[MOLIT] 매매 수집 실패 - 지역: {}, 년월: {}, 에러: {}", regionCode, yearMonth, e.getMessage());
            throw new RuntimeException("MOLIT API 수집 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 아파트 전월세 실거래가 수집
     */
    @Transactional
    public int scrapeAptRent(String regionCode, String yearMonth) {
        ScrapingLog scrapingLog = createLog(ScrapingLog.ScrapingSource.MOLIT_APT_RENT, regionCode, yearMonth);

        try {
            String xmlResponse = callMolitApi(molitApiProperties.getAptRentUrl(), regionCode, yearMonth);
            List<MolitTradeDto> dtos = parseRentXml(xmlResponse, regionCode);

            int savedCount = saveRentData(dtos);

            scrapingLog.complete(savedCount);
            scrapingLogRepository.save(scrapingLog);
            log.info("[MOLIT] 전월세 수집 완료 - 지역: {}, 년월: {}, 저장: {}건", regionCode, yearMonth, savedCount);
            return savedCount;

        } catch (Exception e) {
            scrapingLog.fail(e.getMessage());
            scrapingLogRepository.save(scrapingLog);
            log.error("[MOLIT] 전월세 수집 실패 - 지역: {}, 년월: {}, 에러: {}", regionCode, yearMonth, e.getMessage());
            throw new RuntimeException("MOLIT API 수집 실패: " + e.getMessage(), e);
        }
    }

    private String callMolitApi(String apiUrl, String regionCode, String yearMonth) {
        String scheme = apiUrl.startsWith("https") ? "https" : "http";
        return molitWebClient.get()
            .uri(uriBuilder -> uriBuilder
                .scheme(scheme)
                .host(extractHost(apiUrl))
                .path(extractPath(apiUrl))
                .queryParam("serviceKey", molitApiProperties.getServiceKey())
                .queryParam("LAWD_CD", regionCode)
                .queryParam("DEAL_YMD", yearMonth)
                .queryParam("numOfRows", "1000")
                .queryParam("pageNo", "1")
                .build())
            .retrieve()
            .bodyToMono(String.class)
            .block();
    }

    private List<MolitTradeDto> parseTradeXml(String xml, String regionCode) throws Exception {
        List<MolitTradeDto> list = new ArrayList<>();
        Document doc = parseXml(xml);
        NodeList items = doc.getElementsByTagName("item");

        for (int i = 0; i < items.getLength(); i++) {
            var item = items.item(i);
            var childNodes = item.getChildNodes();

            MolitTradeDto dto = new MolitTradeDto();
            dto.setRegionCode(regionCode);

            for (int j = 0; j < childNodes.getLength(); j++) {
                var node = childNodes.item(j);
                String name = node.getNodeName();
                String value = node.getTextContent().trim();

                switch (name) {
                    case "아파트"    -> dto.setApartName(value);
                    case "법정동"    -> dto.setDong(value);
                    case "층"       -> dto.setFloor(value);
                    case "전용면적"  -> dto.setExclusiveArea(parseDouble(value));
                    case "거래금액"  -> dto.setTradePrice(parseMoney(value));
                    case "년"       -> {
                        if (dto.getTradeDate() == null) dto.setTradeDate(LocalDate.of(parseInt(value), 1, 1));
                        else dto.setTradeDate(dto.getTradeDate().withYear(parseInt(value)));
                    }
                    case "월"       -> {
                        if (dto.getTradeDate() != null) dto.setTradeDate(dto.getTradeDate().withMonth(parseInt(value)));
                    }
                    case "일"       -> {
                        if (dto.getTradeDate() != null) dto.setTradeDate(dto.getTradeDate().withDayOfMonth(parseInt(value)));
                    }
                    case "건축년도"  -> dto.setBuildYear(parseInt(value));
                }
            }

            if (dto.getApartName() != null && dto.getTradePrice() != null) {
                list.add(dto);
            }
        }
        return list;
    }

    private List<MolitTradeDto> parseRentXml(String xml, String regionCode) throws Exception {
        List<MolitTradeDto> list = new ArrayList<>();
        Document doc = parseXml(xml);
        NodeList items = doc.getElementsByTagName("item");

        for (int i = 0; i < items.getLength(); i++) {
            var item = items.item(i);
            var childNodes = item.getChildNodes();

            MolitTradeDto dto = new MolitTradeDto();
            dto.setRegionCode(regionCode);

            for (int j = 0; j < childNodes.getLength(); j++) {
                var node = childNodes.item(j);
                String name = node.getNodeName();
                String value = node.getTextContent().trim();

                switch (name) {
                    case "아파트"    -> dto.setApartName(value);
                    case "법정동"    -> dto.setDong(value);
                    case "층"       -> dto.setFloor(value);
                    case "전용면적"  -> dto.setExclusiveArea(parseDouble(value));
                    case "보증금액"  -> dto.setDeposit(parseMoney(value));
                    case "월세금액"  -> dto.setMonthlyRent(parseMoney(value));
                    case "년"       -> {
                        if (dto.getTradeDate() == null) dto.setTradeDate(LocalDate.of(parseInt(value), 1, 1));
                        else dto.setTradeDate(dto.getTradeDate().withYear(parseInt(value)));
                    }
                    case "월"       -> {
                        if (dto.getTradeDate() != null) dto.setTradeDate(dto.getTradeDate().withMonth(parseInt(value)));
                    }
                    case "일"       -> {
                        if (dto.getTradeDate() != null) dto.setTradeDate(dto.getTradeDate().withDayOfMonth(parseInt(value)));
                    }
                }
            }

            if (dto.getApartName() != null && dto.getDeposit() != null) {
                list.add(dto);
            }
        }
        return list;
    }

    private int saveTradeData(List<MolitTradeDto> dtos, PriceHistory.TradeType tradeType) {
        int count = 0;
        for (MolitTradeDto dto : dtos) {
            // 중복 방지
            boolean exists = priceHistoryRepository
                .existsByApartNameAndTradeDateAndExclusiveAreaAndFloorAndTradePrice(
                    dto.getApartName(), dto.getTradeDate(),
                    dto.getExclusiveArea(), dto.getFloor(), dto.getTradePrice()
                );
            if (exists) continue;

            PriceHistory history = PriceHistory.builder()
                .regionCode(dto.getRegionCode())
                .apartName(dto.getApartName())
                .dong(dto.getDong())
                .floor(dto.getFloor())
                .exclusiveArea(dto.getExclusiveArea())
                .tradePrice(dto.getTradePrice())
                .tradeDate(dto.getTradeDate())
                .tradeType(tradeType)
                .build();

            priceHistoryRepository.save(history);
            count++;
        }
        return count;
    }

    private int saveRentData(List<MolitTradeDto> dtos) {
        int count = 0;
        for (MolitTradeDto dto : dtos) {
            boolean isJeonse = dto.getMonthlyRent() == null || dto.getMonthlyRent().compareTo(BigDecimal.ZERO) == 0;

            PriceHistory history = PriceHistory.builder()
                .regionCode(dto.getRegionCode())
                .apartName(dto.getApartName())
                .dong(dto.getDong())
                .floor(dto.getFloor())
                .exclusiveArea(dto.getExclusiveArea())
                .deposit(dto.getDeposit())
                .monthlyRent(dto.getMonthlyRent())
                .tradeDate(dto.getTradeDate())
                .tradeType(isJeonse ? PriceHistory.TradeType.JEONSE : PriceHistory.TradeType.MONTHLY)
                .build();

            priceHistoryRepository.save(history);
            count++;
        }
        return count;
    }

    private ScrapingLog createLog(ScrapingLog.ScrapingSource source, String regionCode, String yearMonth) {
        ScrapingLog log = ScrapingLog.builder()
            .source(source)
            .status(ScrapingLog.ScrapingStatus.RUNNING)
            .targetRegion(regionCode)
            .targetYearMonth(yearMonth)
            .startedAt(LocalDateTime.now())
            .build();
        return scrapingLogRepository.save(log);
    }

    private Document parseXml(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(new InputSource(new StringReader(xml)));
    }

    private String extractHost(String url) {
        return url.replace("http://", "").replace("https://", "").split("/")[0];
    }

    private String extractPath(String url) {
        int idx = url.indexOf("/", url.indexOf("//") + 2);
        return idx >= 0 ? url.substring(idx) : "/";
    }

    private Double parseDouble(String value) {
        try { return Double.parseDouble(value); } catch (Exception e) { return null; }
    }

    private BigDecimal parseMoney(String value) {
        try { return new BigDecimal(value.replace(",", "").trim()); } catch (Exception e) { return null; }
    }

    private int parseInt(String value) {
        try { return Integer.parseInt(value.trim()); } catch (Exception e) { return 0; }
    }
}
