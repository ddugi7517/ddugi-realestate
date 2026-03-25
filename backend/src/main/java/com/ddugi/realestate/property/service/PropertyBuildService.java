package com.ddugi.realestate.property.service;

import com.ddugi.realestate.domain.entity.Property;
import com.ddugi.realestate.domain.repository.PriceHistoryRepository;
import com.ddugi.realestate.domain.repository.PropertyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PropertyBuildService {

    private final PriceHistoryRepository priceHistoryRepository;
    private final PropertyRepository propertyRepository;

    private static final Map<String, String> REGION_CODE_TO_DISTRICT = Map.ofEntries(
        Map.entry("11110", "종로구"),
        Map.entry("11140", "중구"),
        Map.entry("11170", "용산구"),
        Map.entry("11200", "성동구"),
        Map.entry("11215", "광진구"),
        Map.entry("11230", "동대문구"),
        Map.entry("11260", "성북구"),
        Map.entry("11290", "강북구"),
        Map.entry("11305", "도봉구"),
        Map.entry("11320", "노원구"),
        Map.entry("11350", "은평구"),
        Map.entry("11380", "서대문구"),
        Map.entry("11410", "마포구"),
        Map.entry("11440", "양천구"),
        Map.entry("11470", "강서구"),
        Map.entry("11500", "구로구"),
        Map.entry("11530", "금천구"),
        Map.entry("11545", "영등포구"),
        Map.entry("11560", "동작구"),
        Map.entry("11590", "관악구"),
        Map.entry("11620", "서초구"),
        Map.entry("11650", "강남구"),
        Map.entry("11680", "송파구"),
        Map.entry("11710", "강동구"),
        Map.entry("41150", "의정부시"),
        Map.entry("41630", "양주시"),
        Map.entry("41360", "남양주시"),
        Map.entry("41281", "고양시 덕양구"),
        Map.entry("41285", "고양시 일산동구"),
        Map.entry("41287", "고양시 일산서구"),
        Map.entry("41480", "파주시")
    );

    @Transactional
    public int build() {
        List<Object[]> rows = priceHistoryRepository.findDistinctApartments();
        log.info("고유 아파트 후보 {}건 조회 완료", rows.size());

        int savedCount = 0;

        for (Object[] row : rows) {
            String apartName  = (String)  row[0];
            String regionCode = (String)  row[1];
            String dong       = (String)  row[2];

            if (propertyRepository.existsByNameAndRegionCode(apartName, regionCode)) {
                continue;
            }

            String city     = resolveCity(regionCode);
            String district = REGION_CODE_TO_DISTRICT.getOrDefault(regionCode, "알수없음");
            String address  = city + " " + district + " " + (dong != null ? dong : "");

            Property property = Property.builder()
                .name(apartName)
                .regionCode(regionCode)
                .city(city)
                .district(district)
                .dong(dong != null ? dong : "")
                .address(address.trim())
                .buildYear(null)
                .totalHousehold(null)
                .type(Property.PropertyType.APT)
                .build();

            propertyRepository.save(property);
            savedCount++;

            if (savedCount % 100 == 0) {
                log.info("Property 저장 진행 중: {}건 완료", savedCount);
            }
        }

        log.info("Property 빌드 완료: 총 {}건 저장", savedCount);
        return savedCount;
    }

    private String resolveCity(String regionCode) {
        if (regionCode == null) return "알수없음";
        if (regionCode.startsWith("11")) return "서울특별시";
        if (regionCode.startsWith("41")) return "경기도";
        return "알수없음";
    }
}
