package com.xtv.service;

import com.xtv.domain.UserAccount;
import com.xtv.domain.Video;
import com.xtv.repo.VideoEntitlementRepository;
import java.time.Instant;
import org.springframework.stereotype.Service;

@Service
public class AccessService {
  private final VideoEntitlementRepository entitlements;

  public AccessService(VideoEntitlementRepository entitlements) {
    this.entitlements = entitlements;
  }

  public boolean canPlay(Video video, UserAccount user) {
    return switch (video.getAccessType()) {
      case FREE -> true;
      case MEMBER -> user != null && user.getMemberUntil() != null && user.getMemberUntil().isAfter(Instant.now());
      case PPV -> user != null && entitlements.existsByUserIdAndVideoId(user.getId(), video.getId());
    };
  }
}
