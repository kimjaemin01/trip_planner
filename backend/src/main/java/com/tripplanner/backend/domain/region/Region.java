package com.tripplanner.backend.domain.region;

import java.math.BigDecimal;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "region")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Region {

    @Id
    private Integer regionId;

    private Integer areaId;

    private String name;

    private BigDecimal lat;

    private BigDecimal lng;

    private BigDecimal stayMin;

    private BigDecimal stayRec;

    private BigDecimal stayMax;

    private String stayType;

    private Integer baseRegionId;

    private Integer groupId;

    private String description;

    private Boolean isActive;

    private Integer sortOrder;

    private String imageUrl;

    // geog(PostGIS) 컬럼은 거리 계산 단계에서 추가
}
