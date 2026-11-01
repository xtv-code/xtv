package com.xtv.service;

import com.xtv.domain.Category;
import com.xtv.domain.Person;
import com.xtv.domain.Series;
import com.xtv.domain.Tag;
import com.xtv.domain.TranscodeJob;
import com.xtv.domain.Video;
import com.xtv.domain.VideoAsset;
import com.xtv.domain.Vocab;
import com.xtv.dto.Models;
import com.xtv.dto.Requests;
import com.xtv.i18n.LocaleTexts;
import com.xtv.repo.CategoryRepository;
import com.xtv.repo.PersonRepository;
import com.xtv.repo.SeriesRepository;
import com.xtv.repo.TagRepository;
import com.xtv.repo.TranscodeJobRepository;
import com.xtv.repo.UserFavoriteRepository;
import com.xtv.repo.VideoAssetRepository;
import com.xtv.repo.VideoEntitlementRepository;
import com.xtv.repo.VideoRenditionRepository;
import com.xtv.repo.VideoRepository;
import com.xtv.repo.VideoViewDailyRepository;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class VideoAdminService {
  private final VideoRepository videos;
  private final VideoAssetRepository assets;
  private final TranscodeJobRepository jobs;
  private final VideoRenditionRepository renditions;
  private final VideoViewDailyRepository dailyViews;
  private final VideoEntitlementRepository entitlements;
  private final UserFavoriteRepository favorites;
  private final CategoryRepository categories;
  private final TagRepository tags;
  private final PersonRepository people;
  private final SeriesRepository seriesRepo;
  private final MediaStore media;
  private final CatalogService catalog;

  public VideoAdminService(VideoRepository videos, VideoAssetRepository assets, TranscodeJobRepository jobs,
                           VideoRenditionRepository renditions, VideoViewDailyRepository dailyViews,
                           VideoEntitlementRepository entitlements, UserFavoriteRepository favorites,
                           CategoryRepository categories, TagRepository tags, PersonRepository people,
                           SeriesRepository seriesRepo, MediaStore media, CatalogService catalog) {
    this.videos = videos;
    this.assets = assets;
    this.jobs = jobs;
    this.renditions = renditions;
    this.dailyViews = dailyViews;
    this.entitlements = entitlements;
    this.favorites = favorites;
    this.categories = categories;
    this.tags = tags;
    this.people = people;
    this.seriesRepo = seriesRepo;
    this.media = media;
    this.catalog = catalog;
  }

  @Transactional
  public Models.VideoDetail create(Requests.VideoCreate req) {
    Video video = new Video();
    LocaleTexts.Stored stored = LocaleTexts.apply(req.title(), req.names(), 300);
    video.setTitle(stored.primary());
    video.setI18n(stored.json());
    video.setStatus(Vocab.VideoStatus.DRAFT);
    video.setFeatured(false);
    video.setViewCount(0);
    video.setAccessType(Vocab.AccessType.FREE);
    video.setCreatedAt(Instant.now());
    return catalog.detail(videos.save(video).getId());
  }

  @Transactional(readOnly = true)
  public Models.PageResult<Models.VideoCard> list(int page, int size, String title, String accessType, String status,
                                                  String sort, String dir) {
    java.util.List<org.springframework.data.jpa.domain.Specification<com.xtv.domain.Video>> parts = new java.util.ArrayList<>();
    AdminQuery.add(parts, AdminQuery.containsAny(title, "title", "i18n"));
    AdminQuery.add(parts, AdminQuery.eqEnum("accessType", com.xtv.domain.Vocab.AccessType.class, accessType));
    AdminQuery.add(parts, AdminQuery.eqEnum("status", com.xtv.domain.Vocab.VideoStatus.class, status));
    var result = videos.findAll(AdminQuery.and(parts),
        AdminQuery.page(page, size, sort, dir, "id", java.util.Set.of("id", "title", "accessType", "status", "createdAt", "publishedAt", "viewCount")));
    return new Models.PageResult<>(result.getContent().stream().map(catalog::card).toList(),
        result.getNumber() + 1, result.getSize(), result.getTotalElements());
  }

  private static int clamp(int size) {
    return Math.min(Math.max(size, 1), 200);
  }

  private static String pageUrl(String value) {
    String text = value == null ? "" : value.trim();
    if (text.length() > 1000) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "地址太长");
    }
    if (!text.isBlank() && !text.startsWith("http://") && !text.startsWith("https://")) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请填写以 http:// 或 https:// 开头的地址");
    }
    return text;
  }

  @Transactional
  public Models.VideoDetail update(Long id, Requests.VideoUpdate req) {
    Video video = videos.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "影片不存在"));
    if ((req.names() != null && !req.names().isEmpty()) || (req.title() != null && !req.title().isBlank())) {
      LocaleTexts.Stored stored = LocaleTexts.apply(req.title(), req.names(), 300);
      video.setTitle(stored.primary());
      video.setI18n(stored.json());
    }
    if (req.sourceUrl() != null) {
      video.setSourceUrl(pageUrl(req.sourceUrl()));
    }
    if (req.featured() != null) {
      video.setFeatured(req.featured());
    }
    if (req.accessType() != null && !req.accessType().isBlank()) {
      try {
        video.setAccessType(Vocab.AccessType.valueOf(req.accessType()));
      } catch (IllegalArgumentException ex) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "访问类型无效");
      }
    }
    if (video.getAccessType() == Vocab.AccessType.PPV) {
      if (req.priceUsdt() != null) {
        video.setPriceUsdt(req.priceUsdt());
      }
      if (video.getPriceUsdt() == null || video.getPriceUsdt().compareTo(BigDecimal.ZERO) <= 0) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "单集付费需要填写价格");
      }
    } else {
      video.setPriceUsdt(null);
    }
    if (req.seriesId() != null) {
      if (req.seriesId() == 0) {
        video.setSeries(null);
        video.setEpisodeNo(null);
      } else {
        if (req.episodeNo() == null || req.episodeNo() < 1) {
          throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "选择短剧后需要填写集数");
        }
        Series series = seriesRepo.findById(req.seriesId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "剧集不存在"));
        video.setSeries(series);
        video.setEpisodeNo(req.episodeNo());
      }
    } else if (req.episodeNo() != null) {
      video.setEpisodeNo(req.episodeNo());
    }
    if (req.categoryIds() != null) {
      video.setCategories(new HashSet<>(categories.findAllById(req.categoryIds())));
    }
    if (req.tagIds() != null) {
      video.setTags(new HashSet<>(tags.findAllById(req.tagIds())));
    }
    if (req.personIds() != null) {
      video.setPeople(new HashSet<>(people.findAllById(req.personIds())));
    }
    return catalog.detail(video.getId());
  }

  @Transactional
  public Models.VideoDetail publish(Long id, boolean published) {
    Video video = videos.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "影片不存在"));
    if (published) {
      if (video.getStatus() != Vocab.VideoStatus.READY && video.getStatus() != Vocab.VideoStatus.PUBLISHED) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "转码完成前不能发布");
      }
      video.setStatus(Vocab.VideoStatus.PUBLISHED);
      if (video.getPublishedAt() == null) {
        video.setPublishedAt(Instant.now());
      }
    } else if (video.getStatus() == Vocab.VideoStatus.PUBLISHED) {
      video.setStatus(Vocab.VideoStatus.READY);
    }
    return catalog.detail(video.getId());
  }

  @Transactional
  public void delete(Long id) {
    Video video = videos.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "影片不存在"));
    video.getCategories().clear();
    video.getTags().clear();
    video.getPeople().clear();
    video.setSeries(null);
    videos.saveAndFlush(video);
    assets.deleteByVideoId(id);
    renditions.deleteByVideoId(id);
    jobs.deleteByVideoId(id);
    dailyViews.deleteByVideoId(id);
    entitlements.deleteByVideoId(id);
    favorites.deleteByVideoId(id);
    videos.delete(video);
    media.deleteVideoMedia(id);
  }

  @Transactional
  public Models.UploadTicket uploadTicket(Long id) {
    Video video = videos.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "影片不存在"));
    if (media.objectMode()) {
      return new Models.UploadTicket("OBJECT", media.presignOriginalPut(video.getId()), media.originalKey(video.getId()));
    }
    return new Models.UploadTicket("LOCAL", null, media.originalKey(video.getId()));
  }

  @Transactional
  public Models.JobView saveLocalSource(Long id, MultipartFile file) throws IOException {
    if (file == null || file.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请选择文件");
    }
    videos.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "影片不存在"));
    MediaStore.SavedFile saved = media.saveOriginal(id, file.getInputStream());
    return queue(id, saved);
  }

  @Transactional
  public Models.JobView completeObjectUpload(Long id, long size) {
    if (!media.objectMode()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "当前不是对象存储");
    }
    videos.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "影片不存在"));
    String key = media.originalKey(id);
    long actual = media.objectSize(key);
    if (actual <= 0) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "对象存储里还没有片源");
    }
    if (size > 0 && actual != size) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "片源大小和上传文件不一致");
    }
    return queue(id, new MediaStore.SavedFile(key, actual, null));
  }

  @Transactional(readOnly = true)
  public List<Models.JobView> jobs(Long videoId) {
    return jobs.findByVideoIdOrderByIdDesc(videoId).stream().map(this::job).toList();
  }

  private Models.JobView queue(Long videoId, MediaStore.SavedFile saved) {
    Video video = videos.findById(videoId).orElseThrow();
    VideoAsset asset = assets.findByVideoId(videoId).orElseGet(VideoAsset::new);
    asset.setVideoId(videoId);
    asset.setStorageKey(saved.key());
    asset.setSizeBytes(saved.size());
    asset.setChecksum(saved.checksum());
    assets.save(asset);
    video.setStatus(Vocab.VideoStatus.UPLOADED);
    TranscodeJob job = new TranscodeJob();
    job.setVideoId(videoId);
    job.setState(Vocab.JobState.PENDING);
    job.setProgress(0);
    job.setRetryCount(0);
    job.setCreatedAt(Instant.now());
    job.setUpdatedAt(job.getCreatedAt());
    return job(jobs.save(job));
  }

  private Models.JobView job(TranscodeJob job) {
    return new Models.JobView(job.getId(), job.getVideoId(), job.getState().name(), job.getProgress(),
        job.getErrorMessage(), job.getRetryCount(), job.getUpdatedAt());
  }
}
