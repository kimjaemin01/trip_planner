package com.tripplanner.backend.domain.rent;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 광역 × 월 렌트 판정
@Entity
@Table(name = "rent_rule")
@IdClass(RentRuleId.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RentRule {

    @Id
    private Integer areaId;

    @Id
    private Integer month;

    private String judgment;

    private String reason;
}
