package com.tripplanner.backend.domain.rent;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RegionRentExceptionRepository extends JpaRepository<RegionRentException, Integer> {

    List<RegionRentException> findByRegionIdIn(Collection<Integer> regionIds);
}
