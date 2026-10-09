package com.tripplanner.backend.domain.rent;

import java.util.List;

// 광역별 렌트 추천 여부
public record RentAdviceResponse(
        Integer areaId,
        String areaName,
        String judgment,      // RECOMMEND / OPTIONAL / NOT_RECOMMEND
        String reason,        // 한줄 근거
        Integer basedOnMonth, // 판정에 쓰인 월 (여러 달 걸치면 가장 보수적인 달)
        List<RegionException> exceptions
) {
    // 세부 지역 예외
    public record RegionException(
            Integer regionId,
            String regionName,
            String judgment,
            String reason
    ) {
    }
}
