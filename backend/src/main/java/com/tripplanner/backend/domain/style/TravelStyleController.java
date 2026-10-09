package com.tripplanner.backend.domain.style;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/styles")
@RequiredArgsConstructor
public class TravelStyleController {

    private final TravelStyleRepository travelStyleRepository;

    @GetMapping
    public List<TravelStyleResponse> getStyles() {
        return travelStyleRepository.findAll(Sort.by("styleId")).stream()
                .map(TravelStyleResponse::from)
                .toList();
    }
}
