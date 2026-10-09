package com.tripplanner.backend.domain.rent;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.tripplanner.backend.domain.area.Area;
import com.tripplanner.backend.domain.area.AreaRepository;
import com.tripplanner.backend.domain.region.Region;
import com.tripplanner.backend.domain.region.RegionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RentService {

    private final RentRuleRepository rentRuleRepository;
    private final RegionRentExceptionRepository regionRentExceptionRepository;
    private final AreaRepository areaRepository;
    private final RegionRepository regionRepository;

    /**
     * 렌트 추천 여부
     * - 기준: 광역 × 월 (rent_rule)
     * - 여행이 여러 달에 걸치면 가장 보수적인 판정 사용 (비추천 > 선택 > 추천)
     * - 세부 예외: regionIds 주면 그 지역만, 안 주면 해당 광역 전체 예외
     */
    public List<RentAdviceResponse> getRentAdvice(List<Integer> areaIds, List<Integer> regionIds,
                                                  Integer month, LocalDate startDate, LocalDate endDate) {
        if (areaIds == null || areaIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "areaIds 필요");
        }
        Set<Integer> months = resolveMonths(month, startDate, endDate);

        Map<Integer, Area> areaById = areaRepository.findAllById(areaIds).stream()
                .collect(Collectors.toMap(Area::getAreaId, Function.identity()));

        Map<Integer, List<RentRule>> rulesByArea = rentRuleRepository
                .findByAreaIdInAndMonthIn(areaIds, months).stream()
                .collect(Collectors.groupingBy(RentRule::getAreaId));

        // 예외 대상 지역
        List<Region> targetRegions = (regionIds == null || regionIds.isEmpty())
                ? regionRepository.findByAreaIdInAndIsActiveTrueOrderBySortOrderAsc(areaIds)
                : regionRepository.findAllById(regionIds);
        Map<Integer, Region> regionById = targetRegions.stream()
                .collect(Collectors.toMap(Region::getRegionId, Function.identity()));

        List<RegionRentException> exceptions = regionById.isEmpty()
                ? List.of()
                : regionRentExceptionRepository.findByRegionIdIn(regionById.keySet());

        List<RentAdviceResponse> result = new ArrayList<>();
        for (Integer areaId : areaIds) {
            Area area = areaById.get(areaId);
            if (area == null) {
                continue;
            }

            Optional<RentRule> rule = rulesByArea.getOrDefault(areaId, List.of()).stream()
                    .max(Comparator.comparingInt((RentRule r) -> severity(r.getJudgment()))
                            .thenComparing(RentRule::getMonth, Comparator.reverseOrder()));

            List<RentAdviceResponse.RegionException> areaExceptions = exceptions.stream()
                    .map(e -> regionById.get(e.getRegionId()))
                    .filter(r -> r != null && r.getAreaId().equals(areaId))
                    .map(r -> {
                        RegionRentException e = exceptions.stream()
                                .filter(x -> x.getRegionId().equals(r.getRegionId()))
                                .findFirst().orElseThrow();
                        return new RentAdviceResponse.RegionException(
                                r.getRegionId(), r.getName(), e.getJudgment(), e.getReason());
                    })
                    .toList();

            result.add(new RentAdviceResponse(
                    areaId,
                    area.getName(),
                    rule.map(RentRule::getJudgment).orElse(null),
                    rule.map(RentRule::getReason).orElse("렌트 정보 없음"),
                    rule.map(RentRule::getMonth).orElse(null),
                    areaExceptions
            ));
        }
        return result;
    }

    // month 하나 or 여행 기간(startDate~endDate)에 걸친 월 목록
    private Set<Integer> resolveMonths(Integer month, LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null) {
            if (endDate.isBefore(startDate)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "endDate가 startDate보다 빠름");
            }
            Set<Integer> months = new LinkedHashSet<>();
            LocalDate d = startDate.withDayOfMonth(1);
            while (!d.isAfter(endDate)) {
                months.add(d.getMonthValue());
                d = d.plusMonths(1);
            }
            return months;
        }
        if (month != null && month >= 1 && month <= 12) {
            return Set.of(month);
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "month(1~12) 또는 startDate+endDate 필요");
    }

    // 보수적일수록 큰 값
    private int severity(String judgment) {
        return switch (judgment) {
            case "NOT_RECOMMEND" -> 3;
            case "OPTIONAL" -> 2;
            case "RECOMMEND" -> 1;
            default -> 0;
        };
    }
}
