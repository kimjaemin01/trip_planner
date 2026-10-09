package com.tripplanner.backend.domain.matrix;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RegionTravelMatrixRepository extends JpaRepository<RegionTravelMatrix, RegionTravelMatrixId> {

    // 후보 지역끼리의 이동시간 한 번에 조회
    List<RegionTravelMatrix> findByFromRegionIdInAndToRegionIdIn(Collection<Integer> fromIds, Collection<Integer> toIds);
}
