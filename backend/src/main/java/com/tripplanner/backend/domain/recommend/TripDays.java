package com.tripplanner.backend.domain.recommend;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

// 여행 일수 계산 공통 (추천 모드 / 직접 선택 모드)
final class TripDays {

    static final int MAX_TRIP_DAYS = 30;
    private static final BigDecimal HALF = new BigDecimal("0.5");

    private TripDays() {
    }

    // tripDays(대략 일수) 또는 startDate + endDate(정확한 날짜)
    static int resolve(Integer tripDays, LocalDate start, LocalDate end) {
        if (start != null && end != null) {
            if (end.isBefore(start)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "endDate가 startDate보다 빠름");
            }
            long days = ChronoUnit.DAYS.between(start, end) + 1;
            if (days > MAX_TRIP_DAYS) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "여행 기간은 최대 " + MAX_TRIP_DAYS + "일");
            }
            return (int) days;
        }
        if (tripDays != null) {
            return tripDays;
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "tripDays 또는 startDate+endDate 필요");
    }

    // 사용 가능 일수 = 여행일수 - 1 (첫날·마지막날 공항 이동 반나절씩)
    static BigDecimal usable(int tripDays) {
        return BigDecimal.valueOf(tripDays).subtract(BigDecimal.ONE).max(HALF);
    }
}
