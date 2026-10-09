package com.tripplanner.backend.domain.recommend;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// 추천 동선 요청
// 기간: tripDays(대략 일수) 또는 startDate + endDate(정확한 날짜) 중 하나
public record RecommendRouteRequest(
        @NotNull Integer countryId,
        @NotEmpty List<Integer> areaIds,                 // 가고 싶은 광역 (복수)
        @NotEmpty @Size(max = 2) List<Integer> styleIds, // 여행 스타일 1~2개
        @Min(1) @Max(30) Integer tripDays,
        LocalDate startDate,
        LocalDate endDate
) {
}
