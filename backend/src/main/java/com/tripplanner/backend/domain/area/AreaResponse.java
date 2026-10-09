package com.tripplanner.backend.domain.area;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

public record AreaResponse(
        Integer areaId,
        Integer countryId,
        String name,
        List<String> mainAirports,
        BigDecimal avgStayMin,
        BigDecimal avgStayMax,
        String imageUrl,
        BigDecimal score,          // 광역 추천도 (상위 세부 지역 평균), 스타일 미선택 시 null
        List<String> topRegions    // 추천도 근거: 점수 높은 세부 지역 이름 (최대 3개)
) {
    public static AreaResponse from(Area area, BigDecimal score, List<String> topRegions) {
        // "HND|NRT" → ["HND", "NRT"]
        List<String> airports = area.getMainAirport() == null
                ? List.of()
                : Arrays.stream(area.getMainAirport().split("\\|")).map(String::trim).toList();

        return new AreaResponse(
                area.getAreaId(),
                area.getCountryId(),
                area.getName(),
                airports,
                area.getAvgStayMin(),
                area.getAvgStayMax(),
                area.getImageUrl(),
                score,
                topRegions
        );
    }
}
