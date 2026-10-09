package com.tripplanner.backend.domain.style;

public record TravelStyleResponse(
        Integer styleId,
        String name,
        Integer dailyVisitCount,
        String stayTendency
) {
    public static TravelStyleResponse from(TravelStyle style) {
        return new TravelStyleResponse(
                style.getStyleId(),
                style.getName(),
                style.getDailyVisitCount(),
                style.getStayTendency()
        );
    }
}
