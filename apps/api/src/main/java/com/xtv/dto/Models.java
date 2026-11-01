package com.xtv.dto;

import com.xtv.domain.Vocab;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class Models {
  private Models() {}

  public record NavItem(String name, String slug) {}

  public record NavResponse(List<NavItem> categories, List<NavItem> tags) {}

  public record VideoCard(
      Long id,
      String title,
      String coverUrl,
      Long durationMs,
      long viewCount,
      Instant publishedAt,
      Vocab.AccessType accessType,
      BigDecimal priceUsdt,
      Integer episodeNo,
      String seriesTitle,
      String seriesStatus,
      String seriesSlug,
      String status) {}

  public record PageResult<T>(List<T> items, int page, int pageSize, long total) {}

  public record NamedRef(Long id, String name, String slug) {}

  public record PersonRef(Long id, String name, String slug, String avatarUrl, String kind, Map<String, String> names) {}

  public record VideoDetail(
      Long id,
      String title,
      String coverUrl,
      Long durationMs,
      long viewCount,
      Instant publishedAt,
      String status,
      Vocab.AccessType accessType,
      BigDecimal priceUsdt,
      boolean entitled,
      Integer episodeNo,
      String seriesTitle,
      String seriesStatus,
      String seriesSlug,
      boolean featured,
      Long seriesId,
      List<NamedRef> categories,
      List<NamedRef> tags,
      List<PersonRef> people,
      String sourceUrl,
      Map<String, String> names) {}

  public record PlanView(Long id, String name, int durationDays, BigDecimal priceUsdt, boolean enabled, Map<String, String> names) {}

  public record PlayResult(
      boolean allowed,
      String playUrl,
      Vocab.AccessType accessType,
      BigDecimal priceUsdt,
      List<PlanView> plans) {}

  public record AuthResponse(String token, Long id, String username, String role, Instant memberUntil) {}

  public record MeResponse(Long id, String username, String role, Instant memberUntil, List<Long> ownedVideoIds) {}

  public record OrderView(
      Long id,
      String orderType,
      Long planId,
      Long videoId,
      BigDecimal listPrice,
      BigDecimal payAmount,
      String paymentMethod,
      String state,
      String address,
      boolean paymentSimulate,
      Instant expiresAt,
      Instant createdAt,
      Long userId,
      String payerAddress,
      String txHash,
      String network,
      String checkoutUrl) {}

  public record AdView(Long id, String slot, String imageUrl, String targetUrl, boolean enabled, Instant startAt, Instant endAt, int sortOrder) {}

  public record TagCard(Long id, String name, String slug, long count) {}

  public record CategoryRow(String name, String slug, List<VideoCard> videos) {}

  public record HomeResponse(
      List<VideoCard> featured,
      List<VideoCard> latest,
      List<VideoCard> hot,
      List<TagCard> popularTags,
      List<CategoryRow> categoryRows) {}

  public record PersonCard(Long id, String name, String slug, String avatarUrl, String kind, long works) {}

  public record SeriesCard(Long id, String title, String slug, String coverUrl, String status, int episodes, String sourceUrl, Map<String, String> names) {}

  public record SeriesDetail(SeriesCard series, PageResult<VideoCard> episodes) {}

  public record UploadTicket(String mode, String uploadUrl, String storageKey) {}

  public record JobView(Long id, Long videoId, String state, int progress, String errorMessage, int retryCount, Instant updatedAt) {}

  public record PlatformView(
      String storageMode,
      String deliveryMode,
      String originBase,
      String cdnBase,
      boolean objectConfigured,
      String usdtAddress,
      boolean paymentSimulate,
      boolean tronApiKeySet,
      String siteName,
      String siteTagline,
      String s3Endpoint,
      String s3Region,
      String s3Bucket,
      String s3AccessKey,
      boolean s3SecretKeySet,
      String tronNetwork,
      String bscAddress,
      String bscNetwork) {}

  public record PaymentChannel(String method) {}

  public record ChainReceiverView(String code, String address, String network) {}

  public record SiteView(String siteName, String siteTagline) {}

  public record LegalPage(String body) {}

  public record LegalEditor(Map<String, String> privacy, Map<String, String> terms) {}

  public record ReportView(Long id, Long videoId, String contact, String message, String status, Instant createdAt) {}

  public record TaxonomyView(Long id, String name, String slug, int sortOrder, boolean showInNav, boolean showOnHome, Map<String, String> names) {}

  public record CategoryCard(Long id, String name, String slug, String coverUrl) {}

  public record FavoriteState(boolean saved) {}

  public record UserView(Long id, String username, String role, Instant memberUntil) {}

}
