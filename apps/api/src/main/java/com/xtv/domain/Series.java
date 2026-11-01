package com.xtv.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class Series {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 200)
  private String title;

  @Column(nullable = false, length = 4000)
  private String i18n = "";

  @Column(name = "cover_key", length = 300)
  private String coverKey;

  @Column(name = "source_url", nullable = false, length = 1000)
  private String sourceUrl = "";

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  private Vocab.SeriesStatus status;

  @Column(nullable = false, unique = true, length = 120)
  private String slug;
}
