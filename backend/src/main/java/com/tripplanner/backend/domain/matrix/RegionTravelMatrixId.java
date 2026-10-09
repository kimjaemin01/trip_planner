package com.tripplanner.backend.domain.matrix;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

// region_travel_matrix 복합키 (출발 + 도착 + 교통수단)
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class RegionTravelMatrixId implements Serializable {

    private Integer fromRegionId;

    private Integer toRegionId;

    private String mode;
}
