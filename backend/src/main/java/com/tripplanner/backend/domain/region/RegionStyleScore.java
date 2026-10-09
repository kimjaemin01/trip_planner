package com.tripplanner.backend.domain.region;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "region_style_score")
@IdClass(RegionStyleScoreId.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RegionStyleScore {

    @Id
    private Integer regionId;

    @Id
    private Integer styleId;

    private Integer score;
}
