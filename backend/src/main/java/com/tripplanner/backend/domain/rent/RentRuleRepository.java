package com.tripplanner.backend.domain.rent;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RentRuleRepository extends JpaRepository<RentRule, RentRuleId> {

    List<RentRule> findByAreaIdInAndMonthIn(Collection<Integer> areaIds, Collection<Integer> months);
}
