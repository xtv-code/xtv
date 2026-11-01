package com.xtv.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "video_rendition")
public class VideoRendition {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "video_id", nullable = false)
  private Long videoId;

  @Column(nullable = false)
  private int height;

  @Column(nullable = false)
  private int bandwidth;

  @Column(name = "playlist_key", nullable = false, length = 400)
  private String playlistKey;
}
