package com.xtv.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "audit_log")
public class AuditLog {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  private Long actorId;
  @Column(nullable = false, length = 64)
  private String actorName = "";
  @Column(nullable = false, length = 64)
  private String action;
  @Column(nullable = false, length = 64)
  private String targetType = "";
  @Column(nullable = false, length = 64)
  private String targetId = "";
  @Column(nullable = false, length = 1000)
  private String detail = "";
  @Column(nullable = false, length = 64)
  private String ip = "";
  @Column(nullable = false)
  private Instant createdAt = Instant.now();
}