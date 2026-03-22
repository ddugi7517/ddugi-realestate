package com.ddugi.realestate.domain.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * 매물 기본 정보 (아파트 단지)
 */
@Entity
@Table(name = "property")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class Property {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;           // 단지명

    @Column(nullable = false)
    private String regionCode;     // 법정동코드

    @Column(nullable = false)
    private String city;           // 시/도

    @Column(nullable = false)
    private String district;       // 구/군

    @Column(nullable = false)
    private String dong;           // 동

    private String address;        // 전체 주소

    private Integer buildYear;     // 건축년도

    private Integer totalHousehold; // 총 세대수

    @Enumerated(EnumType.STRING)
    private PropertyType type;     // 아파트, 오피스텔, 빌라 등

    public enum PropertyType {
        APT, OFFICETEL, VILLA, HOUSE
    }
}
