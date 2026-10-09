package com.tripplanner.backend.domain.country;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/countries")
@RequiredArgsConstructor
public class CountryController {

    private final CountryRepository countryRepository;

    @GetMapping
    public List<CountryResponse> getCountries() {
        return countryRepository.findByIsActiveTrueOrderBySortOrderAsc().stream()
                .map(CountryResponse::from)
                .toList();
    }
}
