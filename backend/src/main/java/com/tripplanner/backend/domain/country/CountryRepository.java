package com.tripplanner.backend.domain.country;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CountryRepository extends JpaRepository<Country, Integer> {

    // 공개된 국가만, 정렬순서대로
    List<Country> findByIsActiveTrueOrderBySortOrderAsc();
}
