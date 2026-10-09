package com.tripplanner.backend.domain.recommend;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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

import lombok.RequiredArgsConstructor;

/**
 * 직접 선택 모드 동선 검사
 *
 * 1. 고른 순서 그대로 동선 → 권장 체류일 + 이동시간
 * 2. 일정 넘으면 추천도 낮은 지역부터 최소 체류일까지 줄이기
 * 3. 그래도 넘으면 → 초과 일수 + 가장 먼 구간 안내
 *    + 고른 지역 중 일정 안에 되는 조합을 "많이 겹치는 순"으로 최대 3개 제안 (순서는 유지)
 * 4. 당일치기 지역은 거점이 같이 선택됐으면 거점에서 왕복
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RouteCheckService {

    private static final int MAX_SUGGESTIONS = 3;
    private static final BigDecimal HALF = new BigDecimal("0.5");

    private final RegionRepository regionRepository;
    private final AreaRepository areaRepository;
    private final RegionStyleScoreRepository regionStyleScoreRepository;
    private final RegionTravelMatrixRepository regionTravelMatrixRepository;

    public RouteCheckResponse check(RouteCheckRequest request) {
        int tripDays = TripDays.resolve(request.tripDays(), request.startDate(), request.endDate());
        BigDecimal usableDays = TripDays.usable(tripDays);

        // 순서 유지 + 중복 제거
        List<Integer> ids = request.regionIds().stream().distinct().toList();
        Map<Integer, Region> found = regionRepository.findAllById(ids).stream()
                .filter(r -> Boolean.TRUE.equals(r.getIsActive()))
                .collect(Collectors.toMap(Region::getRegionId, Function.identity()));

        Map<Integer, Area> areaById = areaRepository.findAllById(
                        found.values().stream().map(Region::getAreaId).distinct().toList()).stream()
                .collect(Collectors.toMap(Area::getAreaId, Function.identity()));

        List<Region> selected = ids.stream()
                .map(found::get)
                .filter(Objects::nonNull)
                .filter(r -> {
                    Area a = areaById.get(r.getAreaId());
                    return a != null && a.getCountryId().equals(request.countryId());
                })
                .toList();
        if (selected.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "선택한 지역이 없거나 해당 국가가 아님");
        }

        List<Integer> selectedIds = selected.stream().map(Region::getRegionId).toList();
        List<Integer> styleIds = request.styleIds() == null ? List.of() : request.styleIds();
        Map<Integer, Double> scoreById = styleIds.isEmpty()
                ? Map.of()
                : regionStyleScoreRepository.findByRegionIdInAndStyleIdIn(selectedIds, styleIds).stream()
                        .collect(Collectors.groupingBy(
                                RegionStyleScore::getRegionId,
                                Collectors.averagingInt(RegionStyleScore::getScore)));

        TravelGraph graph = new TravelGraph(
                regionTravelMatrixRepository.findByFromRegionIdInAndToRegionIdIn(selectedIds, selectedIds));

        Ctx ctx = new Ctx(found, areaById, scoreById, graph, usableDays);

        // 1~2. 고른 그대로
        Candidate full = evaluate(selected, ctx);
        PlanResponse fullPlan = toResponse(full, selected, ctx);

        if (full.fits()) {
            BigDecimal free = fullPlan.freeDays();
            String message = full.compressed()
                    ? "일정이 빠듯해 일부 지역 체류일을 줄였어요"
                    : "선택한 동선으로 " + tripDays + "일 일정 가능";
            if (free.compareTo(BigDecimal.ZERO) > 0) {
                message += " · 자유일정 " + free.stripTrailingZeros().toPlainString() + "일";
            }
            return new RouteCheckResponse(tripDays, usableDays, true, full.compressed(), message,
                    BigDecimal.ZERO, longestLeg(full.route(), ctx), fullPlan, List.of());
        }

        // 3. 불가능 → 초과 일수 + 대안
        BigDecimal overDays = full.needDays().subtract(usableDays).max(BigDecimal.ZERO);
        String longest = longestLeg(full.route(), ctx);
        List<RouteCheckResponse.Suggestion> suggestions = suggest(selected, ctx);

        String message = "일정보다 " + overDays.stripTrailingZeros().toPlainString() + "일 초과";
        if (longest != null) {
            message += " · 가장 먼 구간: " + longest;
        }
        if (!suggestions.isEmpty()) {
            List<String> removed = suggestions.get(0).removedRegionNames();
            message += " → " + String.join(", ", removed) + " 빼면 가능";
        }

        return new RouteCheckResponse(tripDays, usableDays, false, true, message,
                overDays, longest, fullPlan, suggestions);
    }

    // ===== 동선 평가 =====

    private Candidate evaluate(List<Region> regions, Ctx ctx) {
        Set<Integer> chosen = regions.stream().map(Region::getRegionId).collect(Collectors.toSet());
        List<Integer> main = regions.stream()
                .filter(r -> !isSideTrip(r, chosen))
                .map(Region::getRegionId)
                .toList();
        Route route = ctx.graph().fixedRoute(main);

        Map<Integer, BigDecimal> stay = new LinkedHashMap<>();
        regions.forEach(r -> stay.put(r.getRegionId(), r.getStayRec()));

        boolean compressed = false;
        if (!fits(stay, route, ctx)) {
            // 추천도 낮은 지역부터 0.5일씩 최소 체류일까지
            List<Region> byLowScore = new ArrayList<>(regions);
            byLowScore.sort(Comparator.comparingDouble(ctx::score));
            boolean changed = true;
            while (!fits(stay, route, ctx) && changed) {
                changed = false;
                for (Region r : byLowScore) {
                    BigDecimal cur = stay.get(r.getRegionId());
                    if (cur.compareTo(r.getStayMin()) > 0) {
                        stay.put(r.getRegionId(), cur.subtract(HALF).max(r.getStayMin()));
                        compressed = true;
                        changed = true;
                        if (fits(stay, route, ctx)) {
                            break;
                        }
                    }
                }
            }
        }

        BigDecimal need = total(stay).add(route.travelDays());
        double score = regions.stream().mapToDouble(ctx::score).sum();
        return new Candidate(regions, stay, route, fits(stay, route, ctx), compressed, need, score);
    }

    // 고른 지역의 부분집합 중 일정 안에 되는 것 (많이 겹치는 순 → 추천도 → 이동시간)
    private List<RouteCheckResponse.Suggestion> suggest(List<Region> selected, Ctx ctx) {
        int n = selected.size();
        List<Candidate> ok = new ArrayList<>();
        for (int mask = (1 << n) - 2; mask > 0; mask--) {
            List<Region> subset = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                if ((mask & (1 << i)) != 0) {
                    subset.add(selected.get(i));
                }
            }
            Candidate c = evaluate(subset, ctx);
            if (c.fits()) {
                ok.add(c);
            }
        }
        ok.sort(Comparator.comparingInt((Candidate c) -> c.regions().size()).reversed()
                .thenComparing(Candidate::score, Comparator.reverseOrder())
                .thenComparingInt(c -> c.route().totalMin()));

        return ok.stream()
                .limit(MAX_SUGGESTIONS)
                .map(c -> new RouteCheckResponse.Suggestion(
                        selected.stream()
                                .filter(r -> !c.regions().contains(r))
                                .map(Region::getName)
                                .toList(),
                        toResponse(c, selected, ctx)))
                .toList();
    }

    private boolean fits(Map<Integer, BigDecimal> stay, Route route, Ctx ctx) {
        return total(stay).add(route.travelDays()).compareTo(ctx.usableDays()) <= 0;
    }

    private BigDecimal total(Map<Integer, BigDecimal> stay) {
        return stay.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private boolean isSideTrip(Region r, Set<Integer> chosen) {
        return r.getBaseRegionId() != null && chosen.contains(r.getBaseRegionId());
    }

    private String longestLeg(Route route, Ctx ctx) {
        int bestIdx = -1;
        for (int i = 1; i < route.legs().size(); i++) {
            if (bestIdx < 0 || route.legs().get(i).minutes() > route.legs().get(bestIdx).minutes()) {
                bestIdx = i;
            }
        }
        if (bestIdx < 0) {
            return null;
        }
        String from = ctx.regionById().get(route.order().get(bestIdx - 1)).getName();
        String to = ctx.regionById().get(route.order().get(bestIdx)).getName();
        int min = route.legs().get(bestIdx).minutes();
        return from + " → " + to + " (" + (min / 60) + "시간 " + (min % 60) + "분)";
    }

    // ===== 응답 변환 =====

    private PlanResponse toResponse(Candidate c, List<Region> requested, Ctx ctx) {
        Set<Integer> chosen = c.stay().keySet();
        Route route = c.route();
        List<StopResponse> stops = new ArrayList<>();
        int no = 1;

        for (int i = 0; i < route.order().size(); i++) {
            Region main = ctx.regionById().get(route.order().get(i));
            stops.add(toStop(no++, main, c, ctx, false, null, route.legs().get(i)));
            for (Region side : c.regions()) {
                if (main.getRegionId().equals(side.getBaseRegionId()) && chosen.contains(side.getRegionId())) {
                    Leg leg = ctx.graph().leg(main.getRegionId(), side.getRegionId());
                    stops.add(toStop(no++, side, c, ctx, true, main.getName(), leg));
                }
            }
        }

        BigDecimal stayDays = total(c.stay());
        BigDecimal travelDays = route.travelDays();
        BigDecimal free = ctx.usableDays().subtract(stayDays).subtract(travelDays).max(BigDecimal.ZERO);

        List<Integer> includedAreas = c.regions().stream().map(Region::getAreaId).distinct().toList();
        List<String> areaNames = includedAreas.stream()
                .map(id -> ctx.areaById().get(id).getName())
                .toList();
        List<String> excludedAreas = requested.stream()
                .map(Region::getAreaId)
                .distinct()
                .filter(id -> !includedAreas.contains(id))
                .map(id -> ctx.areaById().get(id).getName())
                .toList();

        return new PlanResponse(areaNames, excludedAreas, stayDays, route.totalMin(), travelDays, free,
                round(c.score()), stops);
    }

    private StopResponse toStop(int order, Region r, Candidate c, Ctx ctx,
                                boolean dayTrip, String baseName, Leg leg) {
        Area area = ctx.areaById().get(r.getAreaId());
        return new StopResponse(
                order,
                r.getRegionId(),
                r.getName(),
                r.getAreaId(),
                area != null ? area.getName() : null,
                c.stay().get(r.getRegionId()),
                ctx.scoreById().isEmpty() ? null : round(ctx.score(r)),
                r.getDescription(),
                dayTrip,
                baseName,
                leg != null ? leg.mode() : null,
                leg != null ? leg.minutes() : null);
    }

    private BigDecimal round(double value) {
        return BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP);
    }

    // ===== 내부 자료구조 =====

    private record Candidate(List<Region> regions, Map<Integer, BigDecimal> stay, Route route,
                             boolean fits, boolean compressed, BigDecimal needDays, double score) {
    }

    private record Ctx(Map<Integer, Region> regionById, Map<Integer, Area> areaById,
                       Map<Integer, Double> scoreById, TravelGraph graph, BigDecimal usableDays) {

        double score(Region r) {
            return scoreById.getOrDefault(r.getRegionId(), 0.0);
        }
    }
}
