package com.tripplanner.backend.domain.recommend;

import java.math.BigDecimal;
import java.util.List;

// 동선 1개 (최적안 or 대안)
public record PlanResponse(
        List<String> areaNames,          // 포함 광역
        List<String> excludedAreaNames,  // 일정 부족으로 제외된 광역
        BigDecimal stayDays,             // 체류일 합
        int travelMin,                   // 지역 간 이동시간 합(분)
        BigDecimal travelDays,           // 이동에 쓰는 일수 (하루 10시간 기준, 0.5 단위 올림)
        BigDecimal freeDays,             // 남는 일수 (자유일정)
        BigDecimal totalScore,           // 포함 지역 추천도 합
        List<StopResponse> stops
) {
}
