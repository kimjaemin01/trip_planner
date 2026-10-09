package com.tripplanner.backend.domain.style;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "travel_style")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TravelStyle {

    @Id
    private Integer styleId;

    private String name;

    private Integer dailyVisitCount;

    private String stayTendency;
}
