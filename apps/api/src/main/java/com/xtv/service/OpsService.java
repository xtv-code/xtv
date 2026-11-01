package com.xtv.service;

import com.xtv.domain.Advertisement;
import com.xtv.domain.ContentReport;
import com.xtv.domain.LegalDocument;
import com.xtv.domain.MembershipPlan;
import com.xtv.domain.PlatformConfig;
import com.xtv.domain.Vocab;
import com.xtv.dto.Models;
import com.xtv.dto.Requests;
import com.xtv.i18n.LocaleTexts;
import com.xtv.repo.AdvertisementRepository;
import com.xtv.repo.ContentReportRepository;
import com.xtv.repo.ChainReceiverRepository;
import com.xtv.repo.LegalDocumentRepository;
import com.xtv.repo.MembershipPlanRepository;
import com.xtv.repo.PayOrderRepository;
import com.xtv.repo.PlatformConfigRepository;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class OpsService {
  private final AdvertisementRepository ads;
  private final MembershipPlanRepository plans;
  private final PlatformConfigRepository configs;
  private final ContentReportRepository reports;
  private final LegalDocumentRepository legalDocuments;
  private final PayOrderRepository orders;
  private final ChainReceiverRepository receivers;
  private final MediaStore media;
  private final DeliveryService delivery;
  private final AuditService audit;

  public OpsService(AdvertisementRepository ads, MembershipPlanRepository plans, PlatformConfigRepository configs,
                    ContentReportRepository reports, LegalDocumentRepository legalDocuments, PayOrderRepository orders,
                    ChainReceiverRepository receivers, MediaStore media, DeliveryService delivery, AuditService audit) {
    this.ads = ads;
    this.plans = plans;
    this.configs = configs;
    this.reports = reports;
    this.legalDocuments = legalDocuments;
    this.orders = orders;
    this.receivers = receivers;
    this.media = media;
    this.delivery = delivery;
    this.audit = audit;
  }

  @Transactional(readOnly = true)
  public Models.AdView active(Vocab.AdSlot slot) {
    Instant now = Instant.now();
    List<Advertisement> live = ads.findBySlotAndEnabledTrue(slot).stream()
        .filter(ad -> ad.getImageKey() != null && !ad.getImageKey().isBlank())
        .filter(ad -> ad.getStartAt() == null || !ad.getStartAt().isAfter(now))
        .filter(ad -> ad.getEndAt() == null || ad.getEndAt().isAfter(now))
        .toList();
    if (live.isEmpty()) {
      return null;
    }
    return view(live.get(ThreadLocalRandom.current().nextInt(live.size())));
  }

  @Transactional(readOnly = true)
  public List<Models.AdView> allAds() {
    return allAds(null, null, null, "id", "desc");
  }

  @Transactional(readOnly = true)
  public List<Models.AdView> allAds(String slot, String enabled, String targetUrl, String sort, String dir) {
    java.util.List<org.springframework.data.jpa.domain.Specification<com.xtv.domain.Advertisement>> parts = new java.util.ArrayList<>();
    AdminQuery.add(parts, AdminQuery.eqEnum("slot", com.xtv.domain.Vocab.AdSlot.class, slot));
    AdminQuery.add(parts, AdminQuery.eqBool("enabled", enabled));
    AdminQuery.add(parts, AdminQuery.contains("targetUrl", targetUrl));
    return ads.findAll(AdminQuery.and(parts), AdminQuery.sortOf(sort, dir, "id",
            java.util.Set.of("id", "slot", "enabled", "targetUrl", "sortOrder", "startAt", "endAt")))
        .stream().map(this::view).toList();
  }

  @Transactional
  public Models.AdView saveAd(Long id, String slot, String targetUrl, boolean enabled, Instant startAt, Instant endAt,
                              int sortOrder, MultipartFile file) throws IOException {
    Advertisement ad = id == null ? new Advertisement() : ads.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "广告不存在"));
    try {
      ad.setSlot(Vocab.AdSlot.valueOf(slot));
    } catch (Exception ex) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "广告位无效");
    }
    if (targetUrl == null || targetUrl.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "跳转链接不能为空");
    }
    ad.setTargetUrl(targetUrl.trim());
    ad.setEnabled(enabled);
    ad.setStartAt(startAt);
    ad.setEndAt(endAt);
    ad.setSortOrder(sortOrder);
    if (ad.getImageKey() == null) {
      ad.setImageKey("");
    }
    Advertisement saved = ads.save(ad);
    if (file != null && !file.isEmpty()) {
      String key = "ads/" + saved.getId() + ext(file);
      media.writePublic(key, file.getInputStream());
      saved.setImageKey(key);
    }
    if (saved.getImageKey() == null || saved.getImageKey().isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请上传广告图片");
    }
    return view(saved);
  }

  @Transactional
  public void deleteAd(Long id) {
    ads.deleteById(id);
  }

  @Transactional
  public Models.PlanView savePlan(Long id, Requests.PlanRequest req) {
    MembershipPlan plan = id == null ? new MembershipPlan() : plans.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "方案不存在"));
    LocaleTexts.Stored stored = LocaleTexts.apply(req.name(), req.names(), 80);
    if (req.durationDays() == null || req.durationDays() <= 0
        || req.priceUsdt() == null || req.priceUsdt().compareTo(BigDecimal.ZERO) <= 0) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "天数和价格都要填写");
    }
    plan.setName(stored.primary());
    plan.setI18n(stored.json());
    plan.setDurationDays(req.durationDays());
    plan.setPriceUsdt(req.priceUsdt());
    plan.setEnabled(req.enabled() == null || req.enabled());
    MembershipPlan saved = plans.save(plan);
    return planView(saved);
  }

  @Transactional
  public void deletePlan(Long id) {
    if (!plans.existsById(id)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "方案不存在");
    }
    if (orders.existsByPlanId(id)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "已有订单使用这个方案，改为下架即可");
    }
    plans.deleteById(id);
  }

  @Transactional(readOnly = true)
  public List<Models.PlanView> plans(boolean enabledOnly) {
    return plans(enabledOnly, null, null, null, null, "id", "desc");
  }

  @Transactional(readOnly = true)
  public List<Models.PlanView> plans(boolean enabledOnly, String name, String enabled, String durationDays,
                                     String priceUsdt, String sort, String dir) {
    java.util.List<org.springframework.data.jpa.domain.Specification<MembershipPlan>> parts = new java.util.ArrayList<>();
    if (enabledOnly) {
      parts.add((root, query, cb) -> cb.isTrue(root.get("enabled")));
    }
    AdminQuery.add(parts, AdminQuery.containsAny(name, "name", "i18n"));
    AdminQuery.add(parts, AdminQuery.eqBool("enabled", enabled));
    AdminQuery.add(parts, AdminQuery.eqInt("durationDays", durationDays));
    AdminQuery.add(parts, AdminQuery.eqBigDecimal("priceUsdt", priceUsdt));
    return plans.findAll(AdminQuery.and(parts), AdminQuery.sortOf(sort, dir, enabledOnly ? "priceUsdt" : "id",
            java.util.Set.of("id", "name", "enabled", "durationDays", "priceUsdt")))
        .stream().map(this::planView).toList();
  }

  private Models.PlanView planView(MembershipPlan plan) {
    return new Models.PlanView(plan.getId(), LocaleTexts.show(plan.getI18n(), plan.getName()), plan.getDurationDays(),
        plan.getPriceUsdt(), plan.isEnabled(), LocaleTexts.form(plan.getI18n(), plan.getName()));
  }

  @Transactional
  public Models.PlatformView savePlatform(Requests.PlatformRequest req) {
    PlatformConfig cfg = media.config();
    try {
      cfg.setStorageMode(Vocab.StorageMode.valueOf(req.storageMode()));
      cfg.setDeliveryMode(Vocab.DeliveryMode.valueOf(req.deliveryMode()));
    } catch (Exception ex) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "存储或分发模式无效");
    }
    if (req.s3Endpoint() != null) {
      cfg.setS3Endpoint(limit(req.s3Endpoint(), 300, "存储地址过长"));
    }
    if (req.s3Region() != null) {
      String region = req.s3Region().trim();
      cfg.setS3Region(region.isBlank() ? "us-east-1" : limit(region, 64, "区域过长"));
    }
    if (req.s3Bucket() != null) {
      cfg.setS3Bucket(limit(req.s3Bucket(), 128, "桶名称过长"));
    }
    if (req.s3AccessKey() != null) {
      cfg.setS3AccessKey(limit(req.s3AccessKey(), 200, "Access Key 过长"));
    }
    if (req.s3SecretKey() != null && !req.s3SecretKey().isBlank()) {
      cfg.setS3SecretKey(limit(req.s3SecretKey(), 200, "Secret Key 过长"));
    }
    if (cfg.getStorageMode() == Vocab.StorageMode.OBJECT) {
      String endpoint = blank(cfg.getS3Endpoint());
      if (endpoint.isBlank() || blank(cfg.getS3Bucket()).isBlank()
          || blank(cfg.getS3AccessKey()).isBlank() || blank(cfg.getS3SecretKey()).isBlank()) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请填写对象存储的地址、桶、Access Key 和 Secret Key");
      }
      if (!endpoint.startsWith("http://") && !endpoint.startsWith("https://")) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "存储地址需要以 http:// 或 https:// 开头");
      }
    }
    if (req.originBase() == null || req.originBase().isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "源站地址不能为空");
    }
    String cdnBase = req.cdnBase() == null ? "" : req.cdnBase().trim();
    if (cfg.getDeliveryMode() == Vocab.DeliveryMode.CDN && cdnBase.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "选择 CDN 后需要填写 CDN 地址");
    }
    cfg.setOriginBase(req.originBase().trim());
    cfg.setCdnBase(cdnBase);
    boolean simulate = req.paymentSimulate() == null ? cfg.isPaymentSimulate() : req.paymentSimulate();
    String address = req.usdtAddress() == null ? blank(cfg.getUsdtAddress()) : req.usdtAddress().trim();
    String tronKey = req.tronApiKey() == null || req.tronApiKey().isBlank()
        ? blank(cfg.getTronApiKey())
        : req.tronApiKey().trim();
    String bsc = req.bscAddress() == null ? blank(cfg.getBscAddress()) : req.bscAddress().trim();
    boolean chainAddress = receivers.findAll().stream().anyMatch(item -> item.getAddress() != null && !item.getAddress().isBlank());
    if (!simulate && address.isBlank() && bsc.isBlank() && !chainAddress) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请填写至少一个收款地址");
    }
    if (!address.isBlank() && !address.matches("^T[1-9A-HJ-NP-Za-km-z]{33}$")) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请填写 TRC20 收款地址");
    }
    if (!bsc.isBlank() && !bsc.matches("^0x[0-9a-fA-F]{40}$")) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请填写币安链收款地址");
    }
    cfg.setPaymentSimulate(simulate);
    cfg.setUsdtAddress(address);
    cfg.setBscAddress(bsc);
    cfg.setTronApiKey(tronKey);
    if (req.tronNetwork() != null && !req.tronNetwork().isBlank()) {
      try {
        cfg.setTronNetwork(Vocab.TronNetwork.valueOf(req.tronNetwork().trim()));
      } catch (IllegalArgumentException ex) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "网络无效");
      }
    }
    if (req.bscNetwork() != null && !req.bscNetwork().isBlank()) {
      try {
        cfg.setBscNetwork(Vocab.BscNetwork.valueOf(req.bscNetwork().trim()));
      } catch (IllegalArgumentException ex) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "币安链网络无效");
      }
    }
    cfg.setPaymentSeeded(true);
    if (req.siteName() != null) {
      String name = req.siteName().trim();
      if (name.isBlank() || name.length() > 32) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "站点名称需要 1 到 32 个字符");
      }
      cfg.setSiteName(name);
    }
    if (req.siteTagline() != null) {
      String line = req.siteTagline().trim();
      if (line.isBlank() || line.length() > 200) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "站点简介需要 1 到 200 个字符");
      }
      cfg.setSiteTagline(line);
    }
    PlatformConfig saved = configs.save(cfg);
    audit.record("platform.update", "platform", "1",
        "storage=" + saved.getStorageMode() + ",delivery=" + saved.getDeliveryMode() + ",simulate=" + saved.isPaymentSimulate());
    return platform(saved);
  }

  private static String blank(String value) {
    return value == null ? "" : value;
  }

  private static String limit(String value, int max, String message) {
    String text = value == null ? "" : value.trim();
    if (text.length() > max) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
    return text;
  }

  @Transactional(readOnly = true)
  public Models.PlatformView platform() {
    return platform(media.config());
  }

  @Transactional(readOnly = true)
  public Models.LegalPage legal(String code) {
    return new Models.LegalPage(LocaleTexts.show(document(code).getBody(), ""));
  }

  @Transactional(readOnly = true)
  public Models.LegalEditor legalEditor() {
    return new Models.LegalEditor(LocaleTexts.form(document("PRIVACY").getBody(), ""), LocaleTexts.form(document("TERMS").getBody(), ""));
  }

  @Transactional
  public Models.LegalEditor saveLegal(Requests.LegalWrite req) {
    write("PRIVACY", req.privacy());
    write("TERMS", req.terms());
    audit.record("legal.update", "legal", "privacy,terms", "updated");
    return legalEditor();
  }

  private LegalDocument document(String code) {
    String key = "privacy".equalsIgnoreCase(code) ? "PRIVACY" : "terms".equalsIgnoreCase(code) ? "TERMS" : code;
    if (!"PRIVACY".equals(key) && !"TERMS".equals(key)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "协议不存在");
    }
    return legalDocuments.findById(key).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "协议不存在"));
  }

  private void write(String code, java.util.Map<String, String> names) {
    LegalDocument doc = document(code);
    doc.setBody(LocaleTexts.apply(null, names, 8000).json());
    legalDocuments.save(doc);
  }

  @Transactional(readOnly = true)
  public Models.SiteView site() {
    PlatformConfig cfg = media.config();
    return new Models.SiteView(blank(cfg.getSiteName()).isBlank() ? "XTV" : cfg.getSiteName().trim(),
        blank(cfg.getSiteTagline()));
  }

  @Transactional
  public Models.ReportView report(Requests.ReportRequest req) {
    ContentReport report = new ContentReport();
    report.setVideoId(req.videoId());
    report.setContact(req.contact().trim());
    report.setMessage(req.message().trim());
    report.setCreatedAt(Instant.now());
    report.setStatus("OPEN");
    return report(reports.save(report));
  }

  @Transactional
  public Models.ReportView handleReport(Long id) {
    return setReportStatus(id, "HANDLED");
  }

  @Transactional
  public Models.ReportView reopenReport(Long id) {
    return setReportStatus(id, "OPEN");
  }

  @Transactional
  public void deleteReport(Long id) {
    if (!reports.existsById(id)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "投诉不存在");
    }
    reports.deleteById(id);
  }

  private Models.ReportView setReportStatus(Long id, String status) {
    ContentReport report = reports.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "投诉不存在"));
    report.setStatus(status);
    return report(reports.save(report));
  }

  @Transactional(readOnly = true)
  public Models.PageResult<Models.ReportView> reports(int page, int size, String contact, String videoId,
                                                      String status, String createdAt, String message,
                                                      String sort, String dir) {
    java.util.List<org.springframework.data.jpa.domain.Specification<com.xtv.domain.ContentReport>> parts = new java.util.ArrayList<>();
    AdminQuery.add(parts, AdminQuery.contains("contact", contact));
    AdminQuery.add(parts, AdminQuery.eqLong("videoId", videoId));
    AdminQuery.add(parts, AdminQuery.eqString("status", status));
    AdminQuery.add(parts, AdminQuery.instantContains("createdAt", createdAt));
    AdminQuery.add(parts, AdminQuery.contains("message", message));
    var result = reports.findAll(AdminQuery.and(parts),
        AdminQuery.page(page, size, sort, dir, "id",
            java.util.Set.of("id", "contact", "videoId", "status", "createdAt", "message")));
    return new Models.PageResult<>(result.getContent().stream().map(this::report).toList(),
        result.getNumber() + 1, result.getSize(), result.getTotalElements());
  }

  private Models.AdView view(Advertisement ad) {
    return new Models.AdView(ad.getId(), ad.getSlot().name(), delivery.mediaUrl(ad.getImageKey()), ad.getTargetUrl(),
        ad.isEnabled(), ad.getStartAt(), ad.getEndAt(), ad.getSortOrder());
  }

  private Models.PlatformView platform(PlatformConfig cfg) {
    return new Models.PlatformView(cfg.getStorageMode().name(), cfg.getDeliveryMode().name(), cfg.getOriginBase(),
        cfg.getCdnBase(), media.objectConfigured(), blank(cfg.getUsdtAddress()), cfg.isPaymentSimulate(),
        !blank(cfg.getTronApiKey()).isBlank(), blank(cfg.getSiteName()).isBlank() ? "XTV" : cfg.getSiteName().trim(),
        blank(cfg.getSiteTagline()), blank(cfg.getS3Endpoint()),
        blank(cfg.getS3Region()).isBlank() ? "us-east-1" : cfg.getS3Region().trim(),
        blank(cfg.getS3Bucket()), blank(cfg.getS3AccessKey()), !blank(cfg.getS3SecretKey()).isBlank(),
        cfg.getTronNetwork() == null ? Vocab.TronNetwork.MAINNET.name() : cfg.getTronNetwork().name(),
        blank(cfg.getBscAddress()),
        cfg.getBscNetwork() == null ? Vocab.BscNetwork.MAINNET.name() : cfg.getBscNetwork().name());
  }

  private Models.ReportView report(ContentReport report) {
    return new Models.ReportView(report.getId(), report.getVideoId(), report.getContact(), report.getMessage(),
        report.getStatus() == null ? "OPEN" : report.getStatus(), report.getCreatedAt());
  }

  private String ext(MultipartFile file) {
    String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
    if (name.endsWith(".png")) return ".png";
    if (name.endsWith(".webp")) return ".webp";
    if (name.endsWith(".gif")) return ".gif";
    return ".jpg";
  }
}
