package com.tripplanner.backend.domain.rent;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 세부 지역 렌트 예외 (광역 판정과 다를 때)
@Entity
@Table(name = "region_rent_exception")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RegionRentException {

    @Id
    private Integer regionId;

    private String judgment;

    private String reason;
}
