package com.xtv.repo;

import com.xtv.domain.VideoAsset;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VideoAssetRepository extends JpaRepository<VideoAsset, Long> {
  Optional<VideoAsset> findByVideoId(Long videoId);

  void deleteByVideoId(Long videoId);
}
