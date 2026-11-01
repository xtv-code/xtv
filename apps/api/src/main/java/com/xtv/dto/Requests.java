package com.xtv.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class Requests {
  private Requests() {}

  public record RegisterRequest(
      @NotBlank @Size(min = 3, max = 32) String username,
      @NotBlank @Size(min = 6, max = 72) String password) {}

  public record LoginRequest(@NotBlank String username, @NotBlank String password) {}

  public record VideoCreate(@NotBlank @Size(max = 300) String title, Map<String, String> names) {}

  public record VideoUpdate(
      String title,
      Boolean featured,
      String accessType,
      BigDecimal priceUsdt,
      Long seriesId,
      Integer episodeNo,
      List<Long> categoryIds,
      List<Long> tagIds,
      List<Long> personIds,
      String sourceUrl,
      Map<String, String> names) {}

  public record TaxonomyRequest(String name, String slug, Integer sortOrder, Boolean showInNav, Boolean showOnHome, Map<String, String> names) {}

  public record PersonRequest(@NotBlank String name, String slug, String kind, Map<String, String> names) {}

  public record SeriesRequest(@NotBlank String title, String slug, String status, String sourceUrl, Map<String, String> names) {}

  public record CoverUrlRequest(@NotBlank String url) {}

  public record PlanRequest(String name, Integer durationDays, BigDecimal priceUsdt, Boolean enabled, Map<String, String> names) {}

  public record PlatformRequest(
      String storageMode,
      String deliveryMode,
      String originBase,
      String cdnBase,
      String usdtAddress,
      Boolean paymentSimulate,
      String tronApiKey,
      String siteName,
      String siteTagline,
      String s3Endpoint,
      String s3Region,
      String s3Bucket,
      String s3AccessKey,
      String s3SecretKey,
      String tronNetwork,
      String bscAddress,
      String bscNetwork) {}

  public record OrderRequest(String type, Long planId, Long videoId, String paymentMethod) {}

  public record ChainReceiverRequest(String code, String address, String network) {}

  public record ReportRequest(Long videoId, @NotBlank @Size(max = 200) String contact, @NotBlank @Size(max = 2000) String message) {}

  public record PasswordChangeRequest(@NotBlank String currentPassword, @NotBlank @Size(min = 6, max = 72) String newPassword) {}

  public record PasswordResetRequest(@NotBlank @Size(min = 6, max = 72) String password) {}

  public record AdminUserCreate(
      @NotBlank @Size(min = 3, max = 32) String username,
      @NotBlank @Size(min = 6, max = 72) String password,
      String role) {}

  public record AdminUserUpdate(String role, String password) {}

  public record IdsRequest(List<Long> ids) {}

  public record LegalWrite(Map<String, String> privacy, Map<String, String> terms) {}

  public record AdWrite(String slot, String targetUrl, Boolean enabled, Instant startAt, Instant endAt, Integer sortOrder) {}
}
