package com.tripplanner.backend.domain.region;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

// region_style_score 복합키 (region_id + style_id)
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class RegionStyleScoreId implements Serializable {

    private Integer regionId;

    private Integer styleId;
}
