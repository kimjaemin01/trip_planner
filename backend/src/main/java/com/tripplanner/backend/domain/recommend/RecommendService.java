package com.tripplanner.backend.domain.recommend;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.tripplanner.backend.domain.area.Area;
import com.tripplanner.backend.domain.area.AreaRepository;
import com.tripplanner.backend.domain.matrix.RegionTravelMatrixRepository;
import com.tripplanner.backend.domain.recommend.TravelGraph.Leg;
import com.tripplanner.backend.domain.recommend.TravelGraph.Route;
import com.tripplanner.backend.domain.region.Region;
import com.tripplanner.backend.domain.region.RegionRepository;
import com.tripplanner.backend.domain.region.RegionStyleScore;
import com.tripplanner.backend.domain.region.RegionStyleScoreRepository;
import com.tripplanner.backend.domain.style.TravelStyle;
import com.tripplanner.backend.domain.style.TravelStyleRepository;

import lombok.RequiredArgsConstructor;

/**
 * 추천 동선
 *
 * 1. 사용 가능 일수 = 여행일수 - 1 (첫날·마지막날 공항 이동 반나절씩)
 * 2. 고른 광역의 모든 조합(최대 2^6 = 63개)마다 동선 생성
 *    a. 각 광역에서 추천도 1위 지역을 최소 체류일로 넣기 → 안 들어가면 그 조합 불가
 *    b. 나머지 지역을 추천도 높은 순으로 최소 체류일로 추가 (일정 넘으면 건너뜀)
 *       - 당일치기·묶음 지역은 거점도 함께 추가 (미야지마 → 히로시마)
 *    c. 남는 일수로 체류일 늘리기 (권장 → 최대)
 *       - 여유형 스타일(MAX): b 전에 권장 체류일까지 먼저 늘림 → 적게 가고 오래 머묾
 *       - 압축형 스타일(MIN): 권장 체류일까지만
 * 3. 거점이 일정에 있는 당일치기 지역 → 거점에서 왕복 (동선 순서에서 제외)
 * 4. 정렬: 포함 광역 수 → 추천도 합 → 이동시간 짧은 순
 * 5. 1위 = 최적 동선, 2~3위 = 대안
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecommendService {

    private static final int MAX_ALTERNATIVES = 2;
    private static final BigDecimal HALF = new BigDecimal("0.5");

    private final AreaRepository areaRepository;
    private final RegionRepository regionRepository;
    private final RegionStyleScoreRepository regionStyleScoreRepository;
    private final TravelStyleRepository travelStyleRepository;
    private final RegionTravelMatrixRepository regionTravelMatrixRepository;

    public RecommendRouteResponse recommend(RecommendRouteRequest request) {
        int tripDays = TripDays.resolve(request.tripDays(), request.startDate(), request.endDate());
        BigDecimal usableDays = TripDays.usable(tripDays);

        // 고른 광역 (해당 국가 + 공개된 것만)
        List<Area> areas = areaRepository.findAllById(request.areaIds()).stream()
                .filter(a -> a.getCountryId().equals(request.countryId()) && Boolean.TRUE.equals(a.getIsActive()))
                .sorted(Comparator.comparing(Area::getSortOrder, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        if (areas.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "선택한 광역이 없거나 해당 국가가 아님");
        }

        List<Integer> areaIds = areas.stream().map(Area::getAreaId).toList();
        List<Region> regions = regionRepository.findByAreaIdInAndIsActiveTrueOrderBySortOrderAsc(areaIds);
        List<Integer> regionIds = regions.stream().map(Region::getRegionId).toList();

        Map<Integer, Double> scoreById = regionIds.isEmpty()
                ? Map.of()
                : regionStyleScoreRepository.findByRegionIdInAndStyleIdIn(regionIds, request.styleIds()).stream()
                        .collect(Collectors.groupingBy(
                                RegionStyleScore::getRegionId,
                                Collectors.averagingInt(RegionStyleScore::getScore)));

        TravelGraph graph = new TravelGraph(regionIds.isEmpty()
                ? List.of()
                : regionTravelMatrixRepository.findByFromRegionIdInAndToRegionIdIn(regionIds, regionIds));

        PlanContext ctx = new PlanContext(
                regions.stream().collect(Collectors.groupingBy(Region::getAreaId)),
                regions.stream().collect(Collectors.toMap(Region::getRegionId, Function.identity())),
                areas.stream().collect(Collectors.toMap(Area::getAreaId, Function.identity())),
                scoreById,
                graph,
                usableDays,
                resolveTendency(travelStyleRepository.findAllById(request.styleIds())));

        // 광역 조합별 동선 생성
        List<Plan> plans = new ArrayList<>();
        int n = areas.size();
        for (int mask = 1; mask < (1 << n); mask++) {
            List<Area> subset = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                if ((mask & (1 << i)) != 0) {
                    subset.add(areas.get(i));
                }
            }
            Plan plan = buildPlan(subset, ctx);
            if (plan != null) {
                plans.add(plan);
            }
        }

        plans.sort(Comparator.comparingInt((Plan p) -> p.areas().size()).reversed()
                .thenComparing(Plan::totalScore, Comparator.reverseOrder())
                .thenComparingInt(p -> p.route().totalMin()));

        if (plans.isEmpty()) {
            return new RecommendRouteResponse(tripDays, usableDays,
                    tripDays + "일 일정으로는 선택한 지역 방문이 어려움 → 기간을 늘리거나 광역을 줄여주세요",
                    null, List.of());
        }

        Plan best = plans.get(0);
        List<PlanResponse> alternatives = plans.stream()
                .skip(1)
                .limit(MAX_ALTERNATIVES)
                .map(p -> toResponse(p, areas, ctx))
                .toList();

        return new RecommendRouteResponse(tripDays, usableDays,
                summary(tripDays, best, areas), toResponse(best, areas, ctx), alternatives);
    }

    // ===== 동선 생성 =====

    private Plan buildPlan(List<Area> subset, PlanContext ctx) {
        Map<Integer, BigDecimal> stay = new LinkedHashMap<>();
        List<Region> pool = new ArrayList<>();

        // a. 각 광역 추천도 1위 지역 (최소 체류일)
        for (Area area : subset) {
            List<Region> inArea = ctx.regionsByArea().getOrDefault(area.getAreaId(), List.of());
            if (inArea.isEmpty()) {
                return null;
            }
            pool.addAll(inArea);
            Region top = inArea.stream()
                    .max(Comparator.comparingDouble(ctx::score))
                    .orElseThrow();
            addWithBase(stay, top, ctx);
        }

        Route route = ctx.route(stay.keySet());
        if (!fits(stay, route, ctx)) {
            return null;
        }

        // 추천도 높은 순 (같으면 sort_order 순 유지)
        pool.sort(Comparator.comparingDouble((Region r) -> ctx.score(r)).reversed());

        // 여유형: 지역 늘리기 전에 권장 체류일까지 먼저
        if ("MAX".equals(ctx.tendency())) {
            expand(stay, route, pool, ctx, Region::getStayRec);
        }

        // b. 나머지 지역 추가
        for (Region r : pool) {
            if (stay.containsKey(r.getRegionId())) {
                continue;
            }
            List<Integer> added = addWithBase(stay, r, ctx);
            Route candidate = ctx.route(stay.keySet());
            if (fits(stay, candidate, ctx)) {
                route = candidate;
            } else {
                added.forEach(stay::remove);
            }
        }

        // c. 체류일 늘리기
        expand(stay, route, pool, ctx, Region::getStayRec);
        if (!"MIN".equals(ctx.tendency())) {
            expand(stay, route, pool, ctx, Region::getStayMax);
        }

        double totalScore = stay.keySet().stream()
                .mapToDouble(id -> ctx.score(ctx.regionById().get(id)))
                .sum();
        return new Plan(subset, stay, route, totalScore);
    }

    /**
     * 지역 추가 (최소 체류일)
     * - 당일치기·묶음 지역(base_region_id 있음)은 거점도 같이 추가
     *   예: 미야지마 → 히로시마, 나라 → 오사카, 가마쿠라 → 도쿄
     * @return 새로 추가된 지역 id (일정 초과 시 되돌리기용)
     */
    private List<Integer> addWithBase(Map<Integer, BigDecimal> stay, Region r, PlanContext ctx) {
        List<Integer> added = new ArrayList<>();
        if (!stay.containsKey(r.getRegionId())) {
            stay.put(r.getRegionId(), r.getStayMin());
            added.add(r.getRegionId());
        }
        Integer baseId = r.getBaseRegionId();
        if (baseId != null && !stay.containsKey(baseId)) {
            Region base = ctx.regionById().get(baseId);
            if (base != null) {
                stay.put(baseId, base.getStayMin());
                added.add(baseId);
            }
        }
        return added;
    }

    // 추천도 높은 지역부터 0.5일씩 늘리기 (cap까지, 일정 안에서)
    private void expand(Map<Integer, BigDecimal> stay, Route route, List<Region> pool,
                        PlanContext ctx, Function<Region, BigDecimal> cap) {
        boolean changed = true;
        while (changed) {
            changed = false;
            for (Region r : pool) {
                BigDecimal current = stay.get(r.getRegionId());
                if (current == null) {
                    continue;
                }
                BigDecimal limit = cap.apply(r);
                if (current.compareTo(limit) >= 0) {
                    continue;
                }
                stay.put(r.getRegionId(), current.add(HALF).min(limit));
                if (fits(stay, route, ctx)) {
                    changed = true;
                } else {
                    stay.put(r.getRegionId(), current);
                }
            }
        }
    }

    private boolean fits(Map<Integer, BigDecimal> stay, Route route, PlanContext ctx) {
        return totalStay(stay).add(route.travelDays()).compareTo(ctx.usableDays()) <= 0;
    }

    private BigDecimal totalStay(Map<Integer, BigDecimal> stay) {
        return stay.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // ===== 응답 변환 =====

    private PlanResponse toResponse(Plan plan, List<Area> requested, PlanContext ctx) {
        Set<Integer> chosen = plan.stay().keySet();
        Route route = plan.route();
        List<StopResponse> stops = new ArrayList<>();
        int no = 1;

        for (int i = 0; i < route.order().size(); i++) {
            Region main = ctx.regionById().get(route.order().get(i));
            stops.add(toStop(no++, main, plan, ctx, false, null, route.legs().get(i)));

            // 이 거점에서 가는 당일치기 지역
            for (Integer id : chosen) {
                Region side = ctx.regionById().get(id);
                if (main.getRegionId().equals(side.getBaseRegionId())) {
                    Leg leg = ctx.graph().leg(main.getRegionId(), side.getRegionId());
                    stops.add(toStop(no++, side, plan, ctx, true, main.getName(), leg));
                }
            }
        }

        BigDecimal stayDays = totalStay(plan.stay());
        BigDecimal travelDays = route.travelDays();
        BigDecimal freeDays = ctx.usableDays().subtract(stayDays).subtract(travelDays).max(BigDecimal.ZERO);

        List<String> areaNames = plan.areas().stream().map(Area::getName).toList();
        List<String> excluded = requested.stream()
                .filter(a -> !plan.areas().contains(a))
                .map(Area::getName)
                .toList();

        return new PlanResponse(areaNames, excluded, stayDays, route.totalMin(), travelDays, freeDays,
                round(plan.totalScore()), stops);
    }

    private StopResponse toStop(int order, Region r, Plan plan, PlanContext ctx,
                                boolean dayTrip, String baseName, Leg leg) {
        Area area = ctx.areaById().get(r.getAreaId());
        return new StopResponse(
                order,
                r.getRegionId(),
                r.getName(),
                r.getAreaId(),
                area != null ? area.getName() : null,
                plan.stay().get(r.getRegionId()),
                round(ctx.score(r)),
                r.getDescription(),
                dayTrip,
                baseName,
                leg != null ? leg.mode() : null,
                leg != null ? leg.minutes() : null);
    }

    private String summary(int tripDays, Plan best, List<Area> requested) {
        String included = best.areas().stream().map(Area::getName).collect(Collectors.joining(", "));
        String text = tripDays + "일 일정에는 " + included + " 추천";
        List<String> excluded = requested.stream()
                .filter(a -> !best.areas().contains(a))
                .map(Area::getName)
                .toList();
        if (!excluded.isEmpty()) {
            text += " · 일정 부족으로 제외: " + String.join(", ", excluded);
        }
        return text;
    }

    // ===== 입력 처리 =====

    // 스타일 체류 성향: 압축(MIN)과 여유(MAX)가 섞이면 기본(REC)
    private String resolveTendency(List<TravelStyle> styles) {
        Set<String> tendencies = styles.stream().map(TravelStyle::getStayTendency).collect(Collectors.toSet());
        if (tendencies.contains("MIN") && tendencies.contains("MAX")) {
            return "REC";
        }
        if (tendencies.contains("MIN")) {
            return "MIN";
        }
        if (tendencies.contains("MAX")) {
            return "MAX";
        }
        return "REC";
    }

    private BigDecimal round(double value) {
        return BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP);
    }

    // ===== 내부 자료구조 =====

    private record Plan(List<Area> areas, Map<Integer, BigDecimal> stay, Route route, double totalScore) {
    }

    private record PlanContext(
            Map<Integer, List<Region>> regionsByArea,
            Map<Integer, Region> regionById,
            Map<Integer, Area> areaById,
            Map<Integer, Double> scoreById,
            TravelGraph graph,
            BigDecimal usableDays,
            String tendency) {

        double score(Region r) {
            return scoreById.getOrDefault(r.getRegionId(), 0.0);
        }

        // 거점이 일정에 있으면 당일치기 → 동선 순서에서 제외
        boolean isSideTrip(Region r, Set<Integer> chosen) {
            return r.getBaseRegionId() != null && chosen.contains(r.getBaseRegionId());
        }

        Route route(Set<Integer> chosen) {
            List<Integer> main = chosen.stream()
                    .filter(id -> !isSideTrip(regionById.get(id), chosen))
                    .toList();
            return graph.route(main);
        }
    }
}
