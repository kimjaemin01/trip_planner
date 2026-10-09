package com.tripplanner.backend.domain.region;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RegionRepository extends JpaRepository<Region, Integer> {

    // 해당 광역의 공개된 세부 지역만, 정렬순서대로
    List<Region> findByAreaIdAndIsActiveTrueOrderBySortOrderAsc(Integer areaId);

    // 여러 광역의 공개된 세부 지역 한 번에 (광역 추천도 계산용)
    List<Region> findByAreaIdInAndIsActiveTrueOrderBySortOrderAsc(Collection<Integer> areaIds);
}
