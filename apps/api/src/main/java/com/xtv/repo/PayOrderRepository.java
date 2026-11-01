package com.xtv.repo;

import com.xtv.domain.PayOrder;
import com.xtv.domain.Vocab;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface PayOrderRepository extends JpaRepository<PayOrder, Long>, JpaSpecificationExecutor<PayOrder> {
  boolean existsByStateAndPayAmount(Vocab.OrderState state, BigDecimal payAmount);
  boolean existsByTxHash(String txHash);
  List<PayOrder> findByState(Vocab.OrderState state);
  List<PayOrder> findByStateAndExpiresAtBefore(Vocab.OrderState state, Instant expiresAt);
  List<PayOrder> findByUserIdOrderByCreatedAtDesc(Long userId);
  boolean existsByPlanId(Long planId);
}
