package com.xtv.service;

import com.xtv.domain.Category;
import com.xtv.domain.Person;
import com.xtv.domain.Series;
import com.xtv.domain.Tag;
import com.xtv.domain.Vocab;
import com.xtv.dto.Models;
import com.xtv.dto.Requests;
import com.xtv.i18n.LocaleTexts;
import com.xtv.repo.CategoryRepository;
import com.xtv.repo.PersonRepository;
import com.xtv.repo.SeriesRepository;
import com.xtv.repo.TagRepository;
import com.xtv.repo.VideoRepository;
import java.io.IOException;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TaxonomyService {
  private final CategoryRepository categories;
  private final TagRepository tags;
  private final PersonRepository people;
  private final SeriesRepository seriesRepo;
  private final VideoRepository videos;
  private final MediaStore media;
  private final DeliveryService delivery;

  public TaxonomyService(CategoryRepository categories, TagRepository tags, PersonRepository people,
                         SeriesRepository seriesRepo, VideoRepository videos, MediaStore media, DeliveryService delivery) {
    this.categories = categories;
    this.tags = tags;
    this.people = people;
    this.seriesRepo = seriesRepo;
    this.videos = videos;
    this.media = media;
    this.delivery = delivery;
  }

  @Transactional
  public Models.TaxonomyView saveCategory(Long id, Requests.TaxonomyRequest req) {
    Category category = id == null ? new Category() : categories.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "分类不存在"));
    apply(category, req, categories::existsBySlug);
    return view(categories.save(category));
  }

  @Transactional
  public void deleteCategory(Long id) {
    if (videos.countUsingCategory(id) > 0) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "还有影片使用这个分类，先从影片里去掉再删除");
    }
    categories.deleteById(id);
  }

  @Transactional
  public Models.TaxonomyView saveTag(Long id, Requests.TaxonomyRequest req) {
    Tag tag = id == null ? new Tag() : tags.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "标签不存在"));
    LocaleTexts.Stored stored = LocaleTexts.apply(req.name(), req.names(), 80);
    tag.setName(stored.primary());
    tag.setI18n(stored.json());
    tag.setSortOrder(req.sortOrder() == null ? 0 : req.sortOrder());
    tag.setShowInNav(false);
    tag.setShowOnHome(false);
    String wanted = req.slug() == null || req.slug().isBlank() ? stored.primary() : req.slug();
    tag.setSlug(Slugs.unique(wanted, slug -> tags.existsBySlug(slug) && !slug.equals(tag.getSlug())));
    return view(tags.save(tag));
  }

  @Transactional
  public void deleteTag(Long id) {
    if (videos.countUsingTag(id) > 0) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "还有影片使用这个标签，先从影片里去掉再删除");
    }
    tags.deleteById(id);
  }

  @Transactional
  public Models.PersonRef savePerson(Long id, Requests.PersonRequest req) {
    Person person = id == null ? new Person() : people.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "人物不存在"));
    LocaleTexts.Stored stored = LocaleTexts.apply(req.name(), req.names(), 120);
    person.setName(stored.primary());
    person.setI18n(stored.json());
    person.setKind(parseKind(req.kind()));
    String wanted = req.slug() == null || req.slug().isBlank() ? stored.primary() : req.slug();
    person.setSlug(Slugs.unique(wanted, slug -> people.existsBySlug(slug) && !slug.equals(person.getSlug())));
    Person saved = people.save(person);
    return personRef(saved);
  }

  @Transactional
  public Models.PersonRef saveAvatar(Long id, MultipartFile file) throws IOException {
    if (file == null || file.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请选择图片");
    }
    if (file.getSize() > 8_000_000) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "图片太大");
    }
    Person person = people.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "人物不存在"));
    String key = "avatars/" + id + ext(file);
    media.writePublic(key, file.getInputStream());
    person.setAvatarKey(key);
    return personRef(person);
  }

  @Transactional
  public void deletePerson(Long id) {
    if (videos.countUsingPerson(id) > 0) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "还有影片关联这个人物，先从影片里去掉再删除");
    }
    people.deleteById(id);
  }

  @Transactional
  public Models.SeriesCard saveSeries(Long id, Requests.SeriesRequest req) {
    Series series = id == null ? new Series() : seriesRepo.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "剧集不存在"));
    LocaleTexts.Stored stored = LocaleTexts.apply(req.title(), req.names(), 200);
    series.setTitle(stored.primary());
    series.setI18n(stored.json());
    series.setStatus(parseSeries(req.status()));
    if (req.sourceUrl() != null) {
      series.setSourceUrl(pageUrl(req.sourceUrl()));
    } else if (id == null) {
      series.setSourceUrl("");
    }
    String wanted = req.slug() == null || req.slug().isBlank() ? stored.primary() : req.slug();
    series.setSlug(Slugs.unique(wanted, slug -> seriesRepo.existsBySlug(slug) && !slug.equals(series.getSlug())));
    Series saved = seriesRepo.save(series);
    return card(saved);
  }

  @Transactional(readOnly = true)
  public Models.PageResult<Models.SeriesCard> listSeries(int page, int size, String title, String status,
                                                         String episodes, String sort, String dir) {
    java.util.List<org.springframework.data.jpa.domain.Specification<com.xtv.domain.Series>> parts = new java.util.ArrayList<>();
    AdminQuery.add(parts, AdminQuery.containsAny(title, "title", "i18n"));
    AdminQuery.add(parts, AdminQuery.eqEnum("status", com.xtv.domain.Vocab.SeriesStatus.class, status));
    if (!AdminQuery.blank(episodes)) {
      try {
        long count = Long.parseLong(AdminQuery.text(episodes));
        parts.add((root, query, cb) -> {
          var sub = query.subquery(Long.class);
          var video = sub.from(com.xtv.domain.Video.class);
          sub.select(cb.count(video));
          sub.where(cb.equal(video.get("series").get("id"), root.get("id")));
          return cb.equal(sub, count);
        });
      } catch (NumberFormatException ex) {
        parts.add((root, query, cb) -> cb.disjunction());
      }
    }
    String safeSort = "episodes".equals(sort) ? "id" : sort;
    var result = seriesRepo.findAll(AdminQuery.and(parts),
        AdminQuery.page(page, size, safeSort, dir, "id", java.util.Set.of("id", "title", "status")));
    return new Models.PageResult<>(result.getContent().stream().map(this::card).toList(),
        result.getNumber() + 1, result.getSize(), result.getTotalElements());
  }

  @Transactional(readOnly = true)
  public Models.PageResult<Models.PersonRef> listPeople(int page, int size, String name, String kind,
                                                        String sort, String dir) {
    java.util.List<org.springframework.data.jpa.domain.Specification<com.xtv.domain.Person>> parts = new java.util.ArrayList<>();
    AdminQuery.add(parts, AdminQuery.containsAny(name, "name", "i18n"));
    AdminQuery.add(parts, AdminQuery.eqEnum("kind", com.xtv.domain.Vocab.PersonKind.class, kind));
    var result = people.findAll(AdminQuery.and(parts),
        AdminQuery.page(page, size, sort, dir, "name", java.util.Set.of("id", "name", "kind")));
    return new Models.PageResult<>(result.getContent().stream().map(this::personRef).toList(),
        result.getNumber() + 1, result.getSize(), result.getTotalElements());
  }

  private static int clamp(int size) {
    return Math.min(Math.max(size, 1), 200);
  }

  @Transactional
  public Models.SeriesCard saveCover(Long id, MultipartFile file) throws IOException {
    Series series = seriesRepo.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "剧集不存在"));
    String key = "series/" + id + ext(file);
    media.writePublic(key, file.getInputStream());
    series.setCoverKey(key);
    return card(seriesRepo.save(series));
  }

  @Transactional
  public void deleteSeries(Long id) {
    if (videos.countBySeriesId(id) > 0) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "这个短剧还有集数，先把集数解绑再删除");
    }
    seriesRepo.deleteById(id);
  }

  private void apply(Category category, Requests.TaxonomyRequest req, java.util.function.Predicate<String> exists) {
    LocaleTexts.Stored stored = LocaleTexts.apply(req.name(), req.names(), 80);
    category.setName(stored.primary());
    category.setI18n(stored.json());
    category.setSortOrder(req.sortOrder() == null ? 0 : req.sortOrder());
    category.setShowInNav(Boolean.TRUE.equals(req.showInNav()));
    category.setShowOnHome(Boolean.TRUE.equals(req.showOnHome()));
    String wanted = req.slug() == null || req.slug().isBlank() ? stored.primary() : req.slug();
    category.setSlug(Slugs.unique(wanted, slug -> exists.test(slug) && !slug.equals(category.getSlug())));
  }

  private Models.PersonRef personRef(Person person) {
    return new Models.PersonRef(person.getId(), LocaleTexts.show(person.getI18n(), person.getName()), person.getSlug(),
        delivery.mediaUrl(person.getAvatarKey()), person.getKind().name(), LocaleTexts.form(person.getI18n(), person.getName()));
  }

  private Models.TaxonomyView view(Category category) {
    return new Models.TaxonomyView(category.getId(), LocaleTexts.show(category.getI18n(), category.getName()), category.getSlug(), category.getSortOrder(),
        category.isShowInNav(), category.isShowOnHome(), LocaleTexts.form(category.getI18n(), category.getName()));
  }

  private Models.TaxonomyView view(Tag tag) {
    return new Models.TaxonomyView(tag.getId(), LocaleTexts.show(tag.getI18n(), tag.getName()), tag.getSlug(), tag.getSortOrder(), tag.isShowInNav(), tag.isShowOnHome(),
        LocaleTexts.form(tag.getI18n(), tag.getName()));
  }

  private Models.SeriesCard card(Series item) {
    return new Models.SeriesCard(item.getId(), LocaleTexts.show(item.getI18n(), item.getTitle()), item.getSlug(), delivery.mediaUrl(item.getCoverKey()),
        item.getStatus().name(), (int) videos.countBySeriesId(item.getId()),
        item.getSourceUrl() == null ? "" : item.getSourceUrl(), LocaleTexts.form(item.getI18n(), item.getTitle()));
  }

  private Vocab.PersonKind parseKind(String kind) {
    if (kind == null || kind.isBlank()) {
      return Vocab.PersonKind.PERFORMER;
    }
    try {
      return Vocab.PersonKind.valueOf(kind);
    } catch (IllegalArgumentException ex) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "人物类型无效");
    }
  }

  private Vocab.SeriesStatus parseSeries(String status) {
    if (status == null || status.isBlank()) {
      return Vocab.SeriesStatus.ONGOING;
    }
    try {
      return Vocab.SeriesStatus.valueOf(status);
    } catch (IllegalArgumentException ex) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "剧集状态无效");
    }
  }

  public Models.SeriesCard setCover(Long id, String key) {
    Series series = seriesRepo.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "剧集不存在"));
    series.setCoverKey(key);
    return card(seriesRepo.save(series));
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

  private String ext(MultipartFile file) {
    String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
    if (name.endsWith(".png")) return ".png";
    if (name.endsWith(".webp")) return ".webp";
    if (name.endsWith(".gif")) return ".gif";
    return ".jpg";
  }
}
