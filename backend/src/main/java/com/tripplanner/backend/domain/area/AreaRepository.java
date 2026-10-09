package com.tripplanner.backend.domain.area;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AreaRepository extends JpaRepository<Area, Integer> {

    // 해당 국가의 공개된 광역만, 정렬순서대로
    List<Area> findByCountryIdAndIsActiveTrueOrderBySortOrderAsc(Integer countryId);
}
