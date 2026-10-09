package com.tripplanner.backend.domain.area;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tripplanner.backend.domain.region.Region;
import com.tripplanner.backend.domain.region.RegionRepository;
import com.tripplanner.backend.domain.region.RegionStyleScore;
import com.tripplanner.backend.domain.region.RegionStyleScoreRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AreaService {

    // 광역 추천도 계산에 쓰는 상위 세부 지역 개수
    private static final int TOP_REGION_COUNT = 3;

    private final AreaRepository areaRepository;
    private final RegionRepository regionRepository;
    private final RegionStyleScoreRepository regionStyleScoreRepository;

    /**
     * 국가 내 광역 목록 + 스타일 추천도
     * - 세부 지역 추천도 = 선택 스타일 점수 평균
     * - 광역 추천도 = 소속 세부 지역 중 상위 3개 추천도 평균 (소수 1자리)
     * - 정렬: 추천도 높은 순 → 같으면 sort_order 순
     * - 스타일 미선택: 추천도 null, sort_order 순
     */
    public List<AreaResponse> getAreas(Integer countryId, List<Integer> styleIds) {
        List<Area> areas = areaRepository.findByCountryIdAndIsActiveTrueOrderBySortOrderAsc(countryId);

        if (styleIds == null || styleIds.isEmpty() || areas.isEmpty()) {
            return areas.stream()
                    .map(area -> AreaResponse.from(area, null, List.of()))
                    .toList();
        }

        List<Integer> areaIds = areas.stream().map(Area::getAreaId).toList();
        List<Region> regions = regionRepository.findByAreaIdInAndIsActiveTrueOrderBySortOrderAsc(areaIds);
        List<Integer> regionIds = regions.stream().map(Region::getRegionId).toList();

        // 세부 지역별 추천도
        Map<Integer, Double> regionScore = regionIds.isEmpty()
                ? Map.of()
                : regionStyleScoreRepository.findByRegionIdInAndStyleIdIn(regionIds, styleIds).stream()
                        .collect(Collectors.groupingBy(
                                RegionStyleScore::getRegionId,
                                Collectors.averagingInt(RegionStyleScore::getScore)));

        // 광역별 세부 지역 묶기
        Map<Integer, List<Region>> regionsByArea = regions.stream()
                .collect(Collectors.groupingBy(Region::getAreaId));

        return areas.stream()
                .map(area -> {
                    List<Region> top = regionsByArea.getOrDefault(area.getAreaId(), List.of()).stream()
                            .filter(r -> regionScore.containsKey(r.getRegionId()))
                            .sorted(Comparator.comparing(
                                    (Region r) -> regionScore.get(r.getRegionId())).reversed())
                            .limit(TOP_REGION_COUNT)
                            .toList();

                    BigDecimal score = top.isEmpty()
                            ? null
                            : round(top.stream()
                                    .mapToDouble(r -> regionScore.get(r.getRegionId()))
                                    .average()
                                    .orElse(0));

                    List<String> topNames = top.stream().map(Region::getName).toList();
                    return AreaResponse.from(area, score, topNames);
                })
                .sorted(Comparator.comparing(AreaResponse::score,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    private BigDecimal round(double value) {
        return BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP);
    }
}
