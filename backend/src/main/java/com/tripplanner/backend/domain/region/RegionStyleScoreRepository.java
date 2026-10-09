package com.tripplanner.backend.domain.region;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RegionStyleScoreRepository extends JpaRepository<RegionStyleScore, RegionStyleScoreId> {

    // 여러 지역 × 선택 스타일 점수 한 번에 조회
    List<RegionStyleScore> findByRegionIdInAndStyleIdIn(Collection<Integer> regionIds, Collection<Integer> styleIds);
}
