package com.xtv.service;

import com.xtv.config.XtvProperties;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class MediaTokenService {
  private static final long TTL_SECONDS = 2 * 60 * 60;
  private final XtvProperties properties;

  public MediaTokenService(XtvProperties properties) {
    this.properties = properties;
  }

  public SignedPath sign(long videoId) {
    long exp = Instant.now().getEpochSecond() + TTL_SECONDS;
    return new SignedPath(exp, hmac(videoId, exp), videoId);
  }

  public void verify(long videoId, long exp, String sig) {
    if (exp < Instant.now().getEpochSecond()) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "播放地址已过期");
    }
    if (sig == null || !constantTimeEquals(sig, hmac(videoId, exp))) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "播放地址无效");
    }
  }

  private String hmac(long videoId, long exp) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(properties.getJwtSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      byte[] raw = mac.doFinal((videoId + ":" + exp).getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(raw);
    } catch (Exception ex) {
      throw new IllegalStateException("无法签发媒体地址", ex);
    }
  }

  private static boolean constantTimeEquals(String left, String right) {
    if (left.length() != right.length()) {
      return false;
    }
    int diff = 0;
    for (int i = 0; i < left.length(); i++) {
      diff |= left.charAt(i) ^ right.charAt(i);
    }
    return diff == 0;
  }

  public record SignedPath(long exp, String sig, long videoId) {
    public String relativeMaster() {
      return "v/" + exp + "/" + sig + "/" + videoId + "/master.m3u8";
    }
  }
}