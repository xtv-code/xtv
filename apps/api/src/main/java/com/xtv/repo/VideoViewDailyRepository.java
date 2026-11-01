package com.xtv.repo;

import com.xtv.domain.VideoViewDaily;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VideoViewDailyRepository extends JpaRepository<VideoViewDaily, Long> {
  Optional<VideoViewDaily> findByVideoIdAndViewDate(Long videoId, LocalDate viewDate);

  void deleteByVideoId(Long videoId);
}
