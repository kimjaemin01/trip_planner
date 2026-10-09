package com.tripplanner.backend.domain.recommend;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// 직접 선택 모드: 사용자가 고른 세부 지역 (방문 순서대로)
public record RouteCheckRequest(
        @NotNull Integer countryId,
        @NotEmpty @Size(max = 15) List<Integer> regionIds, // 방문 순서
        @Size(max = 2) List<Integer> styleIds,             // 선택: 일정 부족 시 어떤 지역을 뺄지 판단
        @Min(1) @Max(30) Integer tripDays,
        LocalDate startDate,
        LocalDate endDate
) {
}
