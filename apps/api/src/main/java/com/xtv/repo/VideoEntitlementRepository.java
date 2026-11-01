package com.xtv.repo;

import com.xtv.domain.VideoEntitlement;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VideoEntitlementRepository extends JpaRepository<VideoEntitlement, Long> {
  boolean existsByUserIdAndVideoId(Long userId, Long videoId);
  List<VideoEntitlement> findByUserId(Long userId);

  void deleteByVideoId(Long videoId);

  void deleteByUserId(Long userId);
}
