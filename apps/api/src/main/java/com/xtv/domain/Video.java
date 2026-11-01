package com.xtv.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class Video {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 300)
  private String title;

  @Column(nullable = false, length = 4000)
  private String i18n = "";

  @Column(name = "cover_key", length = 300)
  private String coverKey;

  @Column(name = "source_url", nullable = false, length = 1000)
  private String sourceUrl = "";

  @Column(name = "duration_ms")
  private Long durationMs;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private Vocab.VideoStatus status;

  @Column(nullable = false)
  private boolean featured;

  @Column(name = "view_count", nullable = false)
  private long viewCount;

  @Column(name = "published_at")
  private Instant publishedAt;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "series_id")
  private Series series;

  @Column(name = "episode_no")
  private Integer episodeNo;

  @Enumerated(EnumType.STRING)
  @Column(name = "access_type", nullable = false, length = 16)
  private Vocab.AccessType accessType;

  @Column(name = "price_usdt", precision = 18, scale = 6)
  private BigDecimal priceUsdt;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @ManyToMany
  @JoinTable(name = "video_category",
      joinColumns = @JoinColumn(name = "video_id"),
      inverseJoinColumns = @JoinColumn(name = "category_id"))
  private Set<Category> categories = new HashSet<>();

  @ManyToMany
  @JoinTable(name = "video_tag",
      joinColumns = @JoinColumn(name = "video_id"),
      inverseJoinColumns = @JoinColumn(name = "tag_id"))
  private Set<Tag> tags = new HashSet<>();

  @ManyToMany
  @JoinTable(name = "video_person",
      joinColumns = @JoinColumn(name = "video_id"),
      inverseJoinColumns = @JoinColumn(name = "person_id"))
  private Set<Person> people = new HashSet<>();
}
