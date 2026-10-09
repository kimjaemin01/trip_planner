package com.tripplanner.backend.domain.recommend;

import java.math.BigDecimal;

// 동선의 경유지 1개
public record StopResponse(
        int order,
        Integer regionId,
        String name,
        Integer areaId,
        String areaName,
        BigDecimal stayDays,      // 체류일 (0.5 = 반일)
        BigDecimal score,         // 스타일 추천도
        String description,       // 한줄 근거
        boolean dayTrip,          // 거점에서 당일치기 여부
        String baseRegionName,    // 당일치기면 출발 거점
        String moveMode,          // 이전 지역(당일치기면 거점)에서 오는 교통수단: TRANSIT / CAR / FLIGHT
        Integer moveMin           // 이동시간(분), 첫 지역은 null
) {
}
