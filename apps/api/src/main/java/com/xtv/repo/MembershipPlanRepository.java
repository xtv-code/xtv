package com.xtv.repo;

import com.xtv.domain.MembershipPlan;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface MembershipPlanRepository extends JpaRepository<MembershipPlan, Long>, JpaSpecificationExecutor<MembershipPlan> {
  List<MembershipPlan> findByEnabledTrueOrderByPriceUsdtAsc();
}
