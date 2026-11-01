package com.xtv.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "video_view_daily")
public class VideoViewDaily {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "video_id", nullable = false)
  private Long videoId;

  @Column(name = "view_date", nullable = false)
  private LocalDate viewDate;

  @Column(name = "view_count", nullable = false)
  private long viewCount;
}
