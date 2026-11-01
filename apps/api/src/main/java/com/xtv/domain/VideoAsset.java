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
@Table(name = "video_asset")
public class VideoAsset {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "video_id", nullable = false, unique = true)
  private Long videoId;

  @Column(name = "storage_key", nullable = false, length = 400)
  private String storageKey;

  @Column(name = "size_bytes", nullable = false)
  private long sizeBytes;

  @Column(length = 128)
  private String checksum;
}
