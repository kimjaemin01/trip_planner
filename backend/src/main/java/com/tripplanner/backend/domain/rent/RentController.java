package com.tripplanner.backend.domain.rent;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class RentController {

    private final RentService rentService;

    // 예: /api/rent-advice?areaIds=1,5&month=7
    // 예: /api/rent-advice?areaIds=5&startDate=2026-10-30&endDate=2026-11-03
    // 예: /api/rent-advice?areaIds=4&regionIds=15,19&month=5
    @GetMapping("/api/rent-advice")
    public List<RentAdviceResponse> getRentAdvice(
            @RequestParam List<Integer> areaIds,
            @RequestParam(required = false) List<Integer> regionIds,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return rentService.getRentAdvice(areaIds, regionIds, month, startDate, endDate);
    }
}
