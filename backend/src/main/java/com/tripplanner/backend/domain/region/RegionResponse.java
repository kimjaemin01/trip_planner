package com.tripplanner.backend.domain.region;

import java.math.BigDecimal;

public record RegionResponse(
        Integer regionId,
        Integer areaId,
        String name,
        BigDecimal lat,
        BigDecimal lng,
        BigDecimal stayMin,
        BigDecimal stayRec,
        BigDecimal stayMax,
        String stayType,
        Integer baseRegionId,
        Integer groupId,
        String description,   // 한줄 근거 (지역 카드 노출용)
        String imageUrl,
        BigDecimal score      // 선택 스타일 추천도 (1.0 ~ 5.0), 스타일 미선택 시 null
) {
    public static RegionResponse from(Region region, BigDecimal score) {
        return new RegionResponse(
                region.getRegionId(),
                region.getAreaId(),
                region.getName(),
                region.getLat(),
                region.getLng(),
                region.getStayMin(),
                region.getStayRec(),
                region.getStayMax(),
                region.getStayType(),
                region.getBaseRegionId(),
                region.getGroupId(),
                region.getDescription(),
                region.getImageUrl(),
                score
        );
    }
}
