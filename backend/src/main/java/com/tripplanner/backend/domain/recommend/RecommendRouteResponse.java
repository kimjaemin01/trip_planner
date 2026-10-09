package com.tripplanner.backend.domain.recommend;

import java.math.BigDecimal;
import java.util.List;

// 추천 동선 응답
public record RecommendRouteResponse(
        int tripDays,
        BigDecimal usableDays,            // 첫날·마지막날 반나절 제외한 일수
        String summary,                   // 예: "5일 일정에는 수도권·간토, 간사이·주고쿠 추천 · 일정 부족으로 제외: 홋카이도"
        PlanResponse best,                // 최적 동선 (불가능하면 null)
        List<PlanResponse> alternatives   // 대안 동선 (최대 2개)
) {
}
