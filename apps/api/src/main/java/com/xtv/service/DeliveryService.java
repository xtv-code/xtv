package com.xtv.service;

import com.xtv.domain.PlatformConfig;
import com.xtv.domain.Vocab;
import org.springframework.stereotype.Service;

@Service
public class DeliveryService {
  private final MediaStore media;
  private final MediaTokenService tokens;

  public DeliveryService(MediaStore media, MediaTokenService tokens) {
    this.media = media;
    this.tokens = tokens;
  }

  /** Public assets: covers, avatars, ads. Never use for HLS playlists. */
  public String mediaUrl(String key) {
    if (key == null || key.isBlank()) {
      return null;
    }
    PlatformConfig cfg = media.config();
    if (cfg.getDeliveryMode() == Vocab.DeliveryMode.CDN && notBlank(cfg.getCdnBase())) {
      return join(cfg.getCdnBase(), key);
    }
    String origin = notBlank(cfg.getOriginBase()) ? cfg.getOriginBase() : "http://localhost:8080";
    return join(origin, "media/" + key);
  }

  /** Entitlement-gated HLS master playlist with path-embedded HMAC. */
  public String playUrl(long videoId) {
    MediaTokenService.SignedPath signed = tokens.sign(videoId);
    PlatformConfig cfg = media.config();
    if (cfg.getDeliveryMode() == Vocab.DeliveryMode.CDN && notBlank(cfg.getCdnBase())) {
      return join(cfg.getCdnBase(), signed.relativeMaster());
    }
    String origin = notBlank(cfg.getOriginBase()) ? cfg.getOriginBase() : "http://localhost:8080";
    return join(origin, "media/" + signed.relativeMaster());
  }

  private static boolean notBlank(String value) {
    return value != null && !value.isBlank();
  }

  private static String join(String base, String path) {
    String left = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
    String right = path.startsWith("/") ? path.substring(1) : path;
    return left + "/" + right;
  }
}