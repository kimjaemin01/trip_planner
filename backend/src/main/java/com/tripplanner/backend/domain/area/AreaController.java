package com.tripplanner.backend.domain.area;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class AreaController {

    private final AreaService areaService;

    // 예: /api/countries/1/areas?styles=6,7
    @GetMapping("/api/countries/{countryId}/areas")
    public List<AreaResponse> getAreas(
            @PathVariable Integer countryId,
            @RequestParam(name = "styles", required = false) List<Integer> styleIds) {
        return areaService.getAreas(countryId, styleIds);
    }
}
