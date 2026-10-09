package com.tripplanner.backend.domain.rent;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

// rent_rule 복합키 (area_id + month)
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class RentRuleId implements Serializable {

    private Integer areaId;

    private Integer month;
}
