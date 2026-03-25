package com.ddugi.realestate.property;

import com.ddugi.realestate.domain.entity.Property;
import com.ddugi.realestate.domain.repository.PropertyRepository;
import com.ddugi.realestate.property.service.PropertyBuildService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/property")
@RequiredArgsConstructor
public class PropertyController {

    private final PropertyBuildService propertyBuildService;
    private final PropertyRepository propertyRepository;

    /**
     * price_history 테이블에서 고유 아파트를 추출해 property 테이블을 채웁니다.
     */
    @PostMapping("/build")
    public ResponseEntity<Map<String, Integer>> build() {
        log.info("POST /api/property/build 요청");
        int savedCount = propertyBuildService.build();
        return ResponseEntity.ok(Map.of("savedCount", savedCount));
    }

    /**
     * 특정 지역코드의 매물 목록을 반환합니다.
     */
    @GetMapping("/list")
    public ResponseEntity<List<Property>> list(@RequestParam String regionCode) {
        log.info("GET /api/property/list?regionCode={}", regionCode);
        List<Property> properties = propertyRepository.findByRegionCode(regionCode);
        return ResponseEntity.ok(properties);
    }

    /**
     * 전체 매물 수를 반환합니다.
     */
    @GetMapping("/count")
    public ResponseEntity<Map<String, Long>> count() {
        long total = propertyRepository.count();
        return ResponseEntity.ok(Map.of("count", total));
    }
}
