package com.tripplanner.backend.domain.area;

import java.math.BigDecimal;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "area")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Area {

    @Id
    private Integer areaId;

    private Integer countryId;

    private String name;

    private String mainAirport;

    private BigDecimal avgStayMin;

    private BigDecimal avgStayMax;

    private Integer sortOrder;

    private Boolean isActive;

    private String imageUrl;
}
