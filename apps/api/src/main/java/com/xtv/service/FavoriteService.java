package com.xtv.service;

import com.xtv.domain.UserAccount;
import com.xtv.domain.UserFavorite;
import com.xtv.domain.Video;
import com.xtv.dto.Models;
import com.xtv.repo.UserFavoriteRepository;
import com.xtv.repo.VideoRepository;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FavoriteService {
  private final UserFavoriteRepository favorites;
  private final VideoRepository videos;
  private final CatalogService catalog;

  public FavoriteService(UserFavoriteRepository favorites, VideoRepository videos, CatalogService catalog) {
    this.favorites = favorites;
    this.videos = videos;
    this.catalog = catalog;
  }

  @Transactional(readOnly = true)
  public List<Models.VideoCard> list(UserAccount user) {
    List<Long> ids = favorites.findByUserIdOrderByCreatedAtDesc(user.getId()).stream().map(UserFavorite::getVideoId).toList();
    if (ids.isEmpty()) {
      return List.of();
    }
    var byId = videos.findByIdIn(ids).stream().collect(java.util.stream.Collectors.toMap(Video::getId, video -> video));
    return ids.stream().filter(byId::containsKey).map(id -> catalog.card(byId.get(id))).toList();
  }

  @Transactional(readOnly = true)
  public boolean saved(UserAccount user, Long videoId) {
    return favorites.findByUserIdAndVideoId(user.getId(), videoId).isPresent();
  }

  @Transactional
  public boolean toggle(UserAccount user, Long videoId) {
    var existing = favorites.findByUserIdAndVideoId(user.getId(), videoId);
    if (existing.isPresent()) {
      favorites.delete(existing.get());
      return false;
    }
    if (videos.findById(videoId).isEmpty()) {
      return false;
    }
    UserFavorite favorite = new UserFavorite();
    favorite.setUserId(user.getId());
    favorite.setVideoId(videoId);
    favorite.setCreatedAt(Instant.now());
    favorites.save(favorite);
    return true;
  }

  @Transactional
  public void sync(UserAccount user, List<Long> ids) {
    if (ids == null || ids.isEmpty()) {
      return;
    }
    var known = new LinkedHashSet<Long>();
    favorites.findByUserIdOrderByCreatedAtDesc(user.getId()).forEach(item -> known.add(item.getVideoId()));
    for (Long id : ids) {
      if (id == null || known.contains(id) || videos.findById(id).isEmpty()) {
        continue;
      }
      UserFavorite favorite = new UserFavorite();
      favorite.setUserId(user.getId());
      favorite.setVideoId(id);
      favorite.setCreatedAt(Instant.now());
      favorites.save(favorite);
      known.add(id);
    }
  }
}
