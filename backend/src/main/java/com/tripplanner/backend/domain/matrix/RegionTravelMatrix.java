package com.tripplanner.backend.domain.matrix;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 지역 간 이동시간 (TRANSIT / CAR / FLIGHT)
@Entity
@Table(name = "region_travel_matrix")
@IdClass(RegionTravelMatrixId.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RegionTravelMatrix {

    @Id
    private Integer fromRegionId;

    @Id
    private Integer toRegionId;

    @Id
    private String mode;

    private Integer durationMin;

    private BigDecimal distanceKm;

    private Integer avgCost;

    private String source;     // ESTIMATE / GOOGLE / MANUAL

    private LocalDateTime updatedAt;
}
