package com.xtv.repo;

import com.xtv.domain.UserFavorite;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserFavoriteRepository extends JpaRepository<UserFavorite, Long> {
  List<UserFavorite> findByUserIdOrderByCreatedAtDesc(Long userId);

  Optional<UserFavorite> findByUserIdAndVideoId(Long userId, Long videoId);

  void deleteByVideoId(Long videoId);

  void deleteByUserId(Long userId);
}
