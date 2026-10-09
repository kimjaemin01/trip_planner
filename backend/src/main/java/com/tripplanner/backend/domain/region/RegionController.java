package com.tripplanner.backend.domain.region;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class RegionController {

    private final RegionService regionService;

    // 예: /api/areas/1/regions?styles=2,3
    @GetMapping("/api/areas/{areaId}/regions")
    public List<RegionResponse> getRegions(
            @PathVariable Integer areaId,
            @RequestParam(name = "styles", required = false) List<Integer> styleIds) {
        return regionService.getRegions(areaId, styleIds);
    }
}
