package com.xtv.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "pay_order")
public class PayOrder {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Enumerated(EnumType.STRING)
  @Column(name = "order_type", nullable = false, length = 16)
  private Vocab.OrderType orderType;

  @Column(name = "plan_id")
  private Long planId;

  @Column(name = "video_id")
  private Long videoId;

  @Column(name = "list_price", nullable = false, precision = 18, scale = 6)
  private BigDecimal listPrice;

  @Column(name = "pay_amount", nullable = false, precision = 18, scale = 6)
  private BigDecimal payAmount;

  @Enumerated(EnumType.STRING)
  @Column(name = "payment_method", nullable = false, length = 32)
  private Vocab.PaymentMethod paymentMethod;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  private Vocab.OrderState state;

  @Column(name = "tx_hash", length = 128)
  private String txHash;

  @Column(name = "payer_address", nullable = false, length = 64)
  private String payerAddress = "";

  @Column(name = "checkout_url", nullable = false, length = 1024)
  private String checkoutUrl = "";

  @Column(name = "external_id", nullable = false, length = 80)
  private String externalId = "";

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;
}
