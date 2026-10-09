package com.tripplanner.backend.domain.recommend;

import java.math.BigDecimal;
import java.util.List;

// 직접 선택 동선 검사 결과
public record RouteCheckResponse(
        int tripDays,
        BigDecimal usableDays,
        boolean feasible,               // 고른 지역 전부 일정 안에 가능?
        boolean compressed,             // 가능하지만 체류일을 줄였는지
        String message,                 // 화면 안내 문구
        BigDecimal overDays,            // 초과 일수 (가능하면 0)
        String longestLeg,              // 가장 오래 걸리는 구간 (예: "도쿄 → 삿포로 (3시간 53분)")
        PlanResponse plan,              // 고른 순서 그대로의 동선
        List<Suggestion> suggestions    // 불가능할 때: 일정 안에 되는 대안 (고른 지역 많이 겹치는 순)
) {
    public record Suggestion(
            List<String> removedRegionNames, // 빠진 지역
            PlanResponse plan
    ) {
    }
}
