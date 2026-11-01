package com.xtv.repo;

import com.xtv.domain.VideoRendition;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VideoRenditionRepository extends JpaRepository<VideoRendition, Long> {
  List<VideoRendition> findByVideoId(Long videoId);
  void deleteByVideoId(Long videoId);
}
