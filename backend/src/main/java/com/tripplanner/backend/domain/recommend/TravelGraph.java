package com.tripplanner.backend.domain.recommend;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.tripplanner.backend.domain.matrix.RegionTravelMatrix;

// 지역 간 이동시간 그래프 + 동선 순서 계산
class TravelGraph {

    static final int DAY_MINUTES = 600;            // 하루 활동시간 10시간
    private static final int NO_ROUTE_MIN = 24 * 60; // 경로 없음 → 사실상 불가로 처리

    record Leg(String mode, int minutes) {
    }

    // order: 방문 순서, legs.get(i): order[i]로 들어오는 이동 (0번은 null)
    record Route(List<Integer> order, List<Leg> legs, int totalMin) {

        BigDecimal travelDays() {
            double halfDays = Math.ceil(totalMin * 2.0 / DAY_MINUTES);
            return BigDecimal.valueOf(halfDays / 2.0).setScale(1, RoundingMode.HALF_UP);
        }
    }

    private final Map<Long, Leg> publicLegs = new HashMap<>(); // TRANSIT, FLIGHT 중 빠른 것
    private final Map<Long, Leg> carLegs = new HashMap<>();    // CAR (대중교통 없을 때만)

    TravelGraph(List<RegionTravelMatrix> rows) {
        for (RegionTravelMatrix m : rows) {
            long key = key(m.getFromRegionId(), m.getToRegionId());
            Leg leg = new Leg(m.getMode(), m.getDurationMin());
            Map<Long, Leg> target = "CAR".equals(m.getMode()) ? carLegs : publicLegs;
            target.merge(key, leg, (a, b) -> a.minutes() <= b.minutes() ? a : b);
        }
    }

    Leg leg(int from, int to) {
        long key = key(from, to);
        Leg leg = publicLegs.get(key);
        if (leg == null) {
            leg = carLegs.get(key);
        }
        return leg != null ? leg : new Leg("NONE", NO_ROUTE_MIN);
    }

    /**
     * 방문 순서 결정
     * - 모든 지역을 출발점으로 한 번씩 → 가장 가까운 곳부터 방문(최근접 이웃)
     * - 총 이동시간이 가장 짧은 순서 선택
     */
    Route route(List<Integer> stops) {
        if (stops.isEmpty()) {
            return new Route(List.of(), List.of(), 0);
        }
        Route best = null;
        for (Integer start : stops) {
            List<Integer> order = new ArrayList<>();
            List<Leg> legs = new ArrayList<>();
            Set<Integer> left = new LinkedHashSet<>(stops);

            order.add(start);
            legs.add(null);
            left.remove(start);

            int total = 0;
            int current = start;
            while (!left.isEmpty()) {
                Integer next = null;
                Leg nextLeg = null;
                for (Integer candidate : left) {
                    Leg l = leg(current, candidate);
                    if (nextLeg == null || l.minutes() < nextLeg.minutes()) {
                        next = candidate;
                        nextLeg = l;
                    }
                }
                order.add(next);
                legs.add(nextLeg);
                left.remove(next);
                total += nextLeg.minutes();
                current = next;
            }
            if (best == null || total < best.totalMin()) {
                best = new Route(order, legs, total);
            }
        }
        return best;
    }

    // 사용자가 정한 순서 그대로 (직접 선택 모드)
    Route fixedRoute(List<Integer> order) {
        List<Leg> legs = new ArrayList<>();
        int total = 0;
        for (int i = 0; i < order.size(); i++) {
            if (i == 0) {
                legs.add(null);
                continue;
            }
            Leg l = leg(order.get(i - 1), order.get(i));
            legs.add(l);
            total += l.minutes();
        }
        return new Route(new ArrayList<>(order), legs, total);
    }

    private static long key(int from, int to) {
        return ((long) from << 32) | (to & 0xffffffffL);
    }
}
