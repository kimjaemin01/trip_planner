package com.tripplanner.backend.domain.recommend;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class RecommendController {

    private final RecommendService recommendService;
    private final RouteCheckService routeCheckService;

    // 추천 모드: 광역 복수 선택 + 스타일 + 기간 → 최적 동선 + 대안
    @PostMapping("/api/recommend/route")
    public RecommendRouteResponse recommendRoute(@Valid @RequestBody RecommendRouteRequest request) {
        return recommendService.recommend(request);
    }

    // 직접 선택 모드: 고른 세부 지역(순서대로) + 기간 → 가능 여부 + 초과 시 대안
    @PostMapping("/api/route/check")
    public RouteCheckResponse checkRoute(@Valid @RequestBody RouteCheckRequest request) {
        return routeCheckService.check(request);
    }
}
