package com.xtv.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "membership_plan")
public class MembershipPlan {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 80)
  private String name;

  @Column(nullable = false, length = 4000)
  private String i18n = "";

  @Column(name = "duration_days", nullable = false)
  private int durationDays;

  @Column(name = "price_usdt", nullable = false, precision = 18, scale = 6)
  private BigDecimal priceUsdt;

  @Column(nullable = false)
  private boolean enabled;
}
