package com.tripplanner.backend.domain.region;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RegionService {

    private final RegionRepository regionRepository;
    private final RegionStyleScoreRepository regionStyleScoreRepository;

    /**
     * 광역 내 세부 지역 목록 + 스타일 추천도
     * - 추천도 = 선택 스타일 점수 평균 (소수 1자리)
     * - 정렬: 추천도 높은 순 → 같으면 sort_order 순
     * - 스타일 미선택: 추천도 null, sort_order 순
     */
    public List<RegionResponse> getRegions(Integer areaId, List<Integer> styleIds) {
        List<Region> regions = regionRepository.findByAreaIdAndIsActiveTrueOrderBySortOrderAsc(areaId);

        if (styleIds == null || styleIds.isEmpty() || regions.isEmpty()) {
            return regions.stream()
                    .map(region -> RegionResponse.from(region, null))
                    .toList();
        }

        List<Integer> regionIds = regions.stream().map(Region::getRegionId).toList();

        Map<Integer, Double> avgScoreByRegion = regionStyleScoreRepository
                .findByRegionIdInAndStyleIdIn(regionIds, styleIds).stream()
                .collect(Collectors.groupingBy(
                        RegionStyleScore::getRegionId,
                        Collectors.averagingInt(RegionStyleScore::getScore)));

        return regions.stream()
                .map(region -> RegionResponse.from(region, toScore(avgScoreByRegion.get(region.getRegionId()))))
                .sorted(Comparator.comparing(RegionResponse::score,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    private BigDecimal toScore(Double avg) {
        if (avg == null) {
            return null;
        }
        return BigDecimal.valueOf(avg).setScale(1, RoundingMode.HALF_UP);
    }
}
