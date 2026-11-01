package com.xtv.service;

import com.xtv.domain.Category;
import com.xtv.domain.Person;
import com.xtv.domain.Series;
import com.xtv.domain.Tag;
import com.xtv.domain.UserAccount;
import com.xtv.domain.Video;
import com.xtv.domain.VideoViewDaily;
import com.xtv.domain.Vocab;
import com.xtv.dto.Models;
import com.xtv.i18n.LocaleTexts;
import com.xtv.repo.CategoryRepository;
import com.xtv.repo.VideoAssetRepository;
import com.xtv.repo.MembershipPlanRepository;
import com.xtv.repo.PersonRepository;
import com.xtv.repo.SeriesRepository;
import com.xtv.repo.TagRepository;
import com.xtv.repo.VideoRepository;
import com.xtv.repo.VideoViewDailyRepository;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CatalogService {
  private final VideoRepository videos;
  private final CategoryRepository categories;
  private final TagRepository tags;
  private final PersonRepository people;
  private final SeriesRepository seriesRepo;
  private final VideoViewDailyRepository dailyViews;
  private final MembershipPlanRepository plans;
  private final DeliveryService delivery;
  private final AccessService access;
  private final VideoAssetRepository assets;
  private final EntityManager em;

  public CatalogService(VideoRepository videos, CategoryRepository categories, TagRepository tags,
                        PersonRepository people, SeriesRepository seriesRepo, VideoViewDailyRepository dailyViews,
                        MembershipPlanRepository plans, DeliveryService delivery, AccessService access,
                        VideoAssetRepository assets, EntityManager em) {
    this.videos = videos;
    this.categories = categories;
    this.tags = tags;
    this.people = people;
    this.seriesRepo = seriesRepo;
    this.dailyViews = dailyViews;
    this.plans = plans;
    this.delivery = delivery;
    this.access = access;
    this.assets = assets;
    this.em = em;
  }

  @Transactional(readOnly = true)
  public Models.NavResponse nav() {
    return new Models.NavResponse(
        categories.findByShowInNavTrueOrderBySortOrderAsc().stream().map(c -> new Models.NavItem(LocaleTexts.show(c.getI18n(), c.getName()), c.getSlug())).toList(),
        List.of());
  }

  @Transactional(readOnly = true)
  public Models.HomeResponse home() {
    var published = Vocab.VideoStatus.PUBLISHED;
    var page = PageRequest.of(0, 12);
    List<Models.CategoryRow> rows = new ArrayList<>();
    for (Category category : categories.findByShowOnHomeTrueOrderBySortOrderAsc()) {
      rows.add(new Models.CategoryRow(LocaleTexts.show(category.getI18n(), category.getName()), category.getSlug(),
          videos.findHomeByCategory(category.getId(), page).stream().map(this::card).toList()));
    }
    return new Models.HomeResponse(
        videos.findByStatusAndFeaturedTrueOrderByPublishedAtDesc(published, page).stream().map(this::card).toList(),
        videos.findByStatusOrderByPublishedAtDesc(published, page).getContent().stream().map(this::card).toList(),
        videos.findByStatusOrderByViewCountDesc(published, page).getContent().stream().map(this::card).toList(),
        popularTags(30),
        rows);
  }

  @Transactional(readOnly = true)
  public Models.PageResult<Models.VideoCard> latest(int page) {
    return pageOf(videos.findByStatusOrderByPublishedAtDesc(Vocab.VideoStatus.PUBLISHED, pageOf(page, 12, "publishedAt")));
  }

  @Transactional(readOnly = true)
  public Models.PageResult<Models.VideoCard> byCategory(String slug, int page) {
    categories.findBySlug(slug).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "分类不存在"));
    return pageOf(videos.findPublishedByCategory(slug, pageOf(page, 12, "publishedAt")));
  }

  @Transactional(readOnly = true)
  public Models.PageResult<Models.VideoCard> byTag(String slug, int page) {
    tags.findBySlug(slug).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "标签不存在"));
    return pageOf(videos.findPublishedByTag(slug, pageOf(page, 12, "publishedAt")));
  }

  @Transactional(readOnly = true)
  public Models.PageResult<Models.VideoCard> search(String q, int page) {
    if (q == null || q.isBlank()) {
      return latest(page);
    }
    return pageOf(videos.search(q.trim(), pageOf(page, 12, "publishedAt")));
  }

  @Transactional(readOnly = true)
  public Models.PageResult<Models.TaxonomyView> categoryList(int page, int size, String name, String showInNav,
                                                             String showOnHome, String sortOrder, String sort, String dir) {
    java.util.List<org.springframework.data.jpa.domain.Specification<com.xtv.domain.Category>> parts = new java.util.ArrayList<>();
    AdminQuery.add(parts, AdminQuery.containsAny(name, "name", "i18n"));
    AdminQuery.add(parts, AdminQuery.eqBool("showInNav", showInNav));
    AdminQuery.add(parts, AdminQuery.eqBool("showOnHome", showOnHome));
    AdminQuery.add(parts, AdminQuery.eqInt("sortOrder", sortOrder));
    var result = categories.findAll(AdminQuery.and(parts),
        AdminQuery.page(page, size, sort, dir, "sortOrder", java.util.Set.of("id", "name", "sortOrder", "showInNav", "showOnHome")));
    return new Models.PageResult<>(result.getContent().stream().map(this::taxonomy).toList(),
        result.getNumber() + 1, result.getSize(), result.getTotalElements());
  }

  @Transactional(readOnly = true)
  public Models.PageResult<Models.CategoryCard> categoryPage(int page) {
    var result = categories.findAll(PageRequest.of(Math.max(page, 1) - 1, 12, Sort.by("sortOrder").and(Sort.by("id"))));
    var coverPage = PageRequest.of(0, 1);
    return new Models.PageResult<>(result.getContent().stream().map(category -> {
      var cover = videos.findHomeByCategory(category.getId(), coverPage).stream()
          .map(video -> delivery.mediaUrl(video.getCoverKey()))
          .findFirst()
          .orElse(null);
      return new Models.CategoryCard(category.getId(), LocaleTexts.show(category.getI18n(), category.getName()), category.getSlug(), cover);
    }).toList(), result.getNumber() + 1, result.getSize(), result.getTotalElements());
  }

  @Transactional(readOnly = true)
  public Models.PageResult<Models.TaxonomyView> tagList(int page, int size, String name, String sortOrder,
                                                        String sort, String dir) {
    java.util.List<org.springframework.data.jpa.domain.Specification<com.xtv.domain.Tag>> parts = new java.util.ArrayList<>();
    AdminQuery.add(parts, AdminQuery.containsAny(name, "name", "i18n"));
    AdminQuery.add(parts, AdminQuery.eqInt("sortOrder", sortOrder));
    var result = tags.findAll(AdminQuery.and(parts),
        AdminQuery.page(page, size, sort, dir, "sortOrder", java.util.Set.of("id", "name", "sortOrder")));
    return new Models.PageResult<>(result.getContent().stream().map(this::tagTaxonomy).toList(),
        result.getNumber() + 1, result.getSize(), result.getTotalElements());
  }

  private static int clamp(int size) {
    return Math.min(Math.max(size, 1), 200);
  }

  @Transactional(readOnly = true)
  public Models.PageResult<Models.TagCard> tags(int page) {
    int safePage = Math.max(page, 1);
    int size = 24;
    String from = """
        from tag t
        join video_tag vt on vt.tag_id = t.id
        join video v on v.id = vt.video_id and v.status = 'PUBLISHED'
        """;
    @SuppressWarnings("unchecked")
    List<Object[]> rows = em.createNativeQuery("""
        select t.id, t.name, t.slug, count(v.id), t.i18n
        """ + from + """
        group by t.id, t.name, t.slug, t.i18n
        order by count(v.id) desc, t.id desc
        """).setFirstResult((safePage - 1) * size).setMaxResults(size).getResultList();
    Number total = (Number) em.createNativeQuery("select count(*) from (select t.id " + from + " group by t.id) counted").getSingleResult();
    List<Models.TagCard> items = rows.stream()
        .map(row -> new Models.TagCard(num(row[0]), LocaleTexts.show(text(row, 4), String.valueOf(row[1])), String.valueOf(row[2]), num(row[3])))
        .toList();
    return new Models.PageResult<>(items, safePage, size, total.longValue());
  }

  @Transactional(readOnly = true)
  public List<Models.TagCard> popularTags(int limit) {
    @SuppressWarnings("unchecked")
    List<Object[]> rows = em.createNativeQuery("""
        select t.id, t.name, t.slug, count(v.id), t.i18n
        from tag t
        join video_tag vt on vt.tag_id = t.id
        join video v on v.id = vt.video_id and v.status = 'PUBLISHED'
        group by t.id, t.name, t.slug, t.i18n
        order by count(v.id) desc
        """).setMaxResults(limit).getResultList();
    return rows.stream().map(row -> new Models.TagCard(num(row[0]), LocaleTexts.show(text(row, 4), String.valueOf(row[1])), String.valueOf(row[2]), num(row[3]))).toList();
  }

  @Transactional(readOnly = true)
  public Models.PageResult<Models.PersonCard> people(Vocab.PersonKind kind, int page, int size) {
    int safePage = Math.max(page, 1);
    String sql = """
        select p.id, p.name, p.slug, p.avatar_key, p.kind, count(v.id), p.i18n
        from person p
        left join video_person vp on vp.person_id = p.id
        left join video v on v.id = vp.video_id and v.status = 'PUBLISHED'
        where p.kind = :kind
        group by p.id, p.name, p.slug, p.avatar_key, p.kind, p.i18n
        order by count(v.id) desc, p.id desc
        """;
    @SuppressWarnings("unchecked")
    List<Object[]> rows = em.createNativeQuery(sql)
        .setParameter("kind", kind.name())
        .setFirstResult((safePage - 1) * size)
        .setMaxResults(size)
        .getResultList();
    Number total = (Number) em.createNativeQuery("select count(*) from person where kind = :kind")
        .setParameter("kind", kind.name()).getSingleResult();
    List<Models.PersonCard> items = rows.stream().map(row -> new Models.PersonCard(
        num(row[0]), LocaleTexts.show(text(row, 6), String.valueOf(row[1])), String.valueOf(row[2]),
        delivery.mediaUrl(row[3] == null ? null : String.valueOf(row[3])),
        String.valueOf(row[4]), num(row[5]))).toList();
    return new Models.PageResult<>(items, safePage, size, total.longValue());
  }

  @Transactional(readOnly = true)
  public Models.PageResult<Models.VideoCard> byPerson(String slug, int page) {
    people.findBySlug(slug).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "人物不存在"));
    return pageOf(videos.findPublishedByPerson(slug, pageOf(page, 12, "publishedAt")));
  }

  @Transactional(readOnly = true)
  public Person personOrThrow(String slug) {
    return people.findBySlug(slug).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "人物不存在"));
  }

  @Transactional(readOnly = true)
  public Models.PageResult<Models.SeriesCard> seriesList(int page) {
    var result = seriesRepo.findAll(PageRequest.of(Math.max(page, 1) - 1, 12, Sort.by(Sort.Direction.DESC, "id")));
    return new Models.PageResult<>(result.getContent().stream().map(this::seriesCard).toList(),
        result.getNumber() + 1, result.getSize(), result.getTotalElements());
  }

  @Transactional(readOnly = true)
  public Models.SeriesDetail series(String slug, int page) {
    Series item = seriesRepo.findBySlug(slug).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "剧集不存在"));
    var episodes = videos.findBySeriesIdAndStatus(item.getId(), Vocab.VideoStatus.PUBLISHED,
        PageRequest.of(Math.max(page, 1) - 1, 12, Sort.by(Sort.Direction.ASC, "episodeNo").and(Sort.by("id"))));
    return new Models.SeriesDetail(seriesCard(item), pageOf(episodes));
  }

  @Transactional(readOnly = true)
  public Models.PageResult<Models.VideoCard> ranking(String range, int page) {
    int safePage = Math.max(page, 1);
    int size = 12;
    if ("all".equals(range) || range == null || range.isBlank()) {
      return pageOf(videos.findByStatusOrderByViewCountDesc(Vocab.VideoStatus.PUBLISHED, PageRequest.of(safePage - 1, size)));
    }
    LocalDate from = switch (range) {
      case "day" -> LocalDate.now(ZoneOffset.UTC);
      case "week" -> LocalDate.now(ZoneOffset.UTC).minusDays(6);
      case "month" -> LocalDate.now(ZoneOffset.UTC).minusDays(29);
      default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "排行范围无效");
    };
    String ranked = """
        select v.id from video v
        join video_view_daily d on d.video_id = v.id
        where v.status = 'PUBLISHED' and d.view_date >= :from
        group by v.id
        order by sum(d.view_count) desc
        """;
    @SuppressWarnings("unchecked")
    List<Number> ids = em.createNativeQuery(ranked)
        .setParameter("from", from)
        .setFirstResult((safePage - 1) * size)
        .setMaxResults(size)
        .getResultList();
    Number total = (Number) em.createNativeQuery("""
        select count(*) from (
          select v.id from video v
          join video_view_daily d on d.video_id = v.id
          where v.status = 'PUBLISHED' and d.view_date >= :from
          group by v.id
        ) t
        """).setParameter("from", from).getSingleResult();
    List<Long> ordered = ids.stream().map(Number::longValue).toList();
    Map<Long, Video> loaded = new HashMap<>();
    videos.findByIdIn(ordered).forEach(video -> loaded.put(video.getId(), video));
    List<Models.VideoCard> items = ordered.stream().map(loaded::get).filter(v -> v != null).map(this::card).toList();
    return new Models.PageResult<>(items, safePage, size, total.longValue());
  }

  @Transactional(readOnly = true)
  public Models.VideoDetail detail(Long id) {
    Video video = visible(id);
    UserAccount user = CurrentUser.optional();
    boolean entitled = access.canPlay(video, user);
    Series series = video.getSeries();
    return new Models.VideoDetail(
        video.getId(), LocaleTexts.show(video.getI18n(), video.getTitle()), delivery.mediaUrl(video.getCoverKey()), video.getDurationMs(),
        video.getViewCount(), video.getPublishedAt(), video.getStatus().name(), video.getAccessType(),
        video.getPriceUsdt(), entitled, video.getEpisodeNo(),
        series == null ? null : LocaleTexts.show(series.getI18n(), series.getTitle()),
        series == null ? null : series.getStatus().name(),
        series == null ? null : series.getSlug(),
        video.isFeatured(),
        series == null ? null : series.getId(),
        video.getCategories().stream().sorted(Comparator.comparingInt(Category::getSortOrder))
            .map(c -> new Models.NamedRef(c.getId(), LocaleTexts.show(c.getI18n(), c.getName()), c.getSlug())).toList(),
        video.getTags().stream().sorted(Comparator.comparingInt(Tag::getSortOrder))
            .map(t -> new Models.NamedRef(t.getId(), LocaleTexts.show(t.getI18n(), t.getName()), t.getSlug())).toList(),
        video.getPeople().stream().sorted(Comparator.comparing(Person::getName))
            .map(p -> new Models.PersonRef(p.getId(), LocaleTexts.show(p.getI18n(), p.getName()), p.getSlug(), delivery.mediaUrl(p.getAvatarKey()), p.getKind().name(), LocaleTexts.form(p.getI18n(), p.getName())))
            .toList(),
        video.getSourceUrl() == null ? "" : video.getSourceUrl(),
        LocaleTexts.form(video.getI18n(), video.getTitle()));
  }

  @Transactional(readOnly = true)
  public Models.PlayResult play(Long id) {
    Video video = visible(id);
    UserAccount user = CurrentUser.optional();
    boolean allowed = access.canPlay(video, user) && (video.getStatus() == Vocab.VideoStatus.PUBLISHED || isAdmin(user));
    boolean ready = assets.findByVideoId(video.getId()).isPresent();
    boolean offerMembership = !allowed && video.getAccessType() == Vocab.AccessType.MEMBER;
    List<Models.PlanView> planViews = offerMembership ? plans.findByEnabledTrueOrderByPriceUsdtAsc().stream()
        .map(plan -> new Models.PlanView(plan.getId(), LocaleTexts.show(plan.getI18n(), plan.getName()), plan.getDurationDays(), plan.getPriceUsdt(), plan.isEnabled(), LocaleTexts.form(plan.getI18n(), plan.getName())))
        .toList() : List.of();
    return new Models.PlayResult(allowed, allowed && ready ? delivery.playUrl(video.getId()) : null,
        video.getAccessType(), video.getPriceUsdt(), planViews);
  }

  @Transactional(readOnly = true)
  public Models.PageResult<Models.VideoCard> related(Long id, int page) {
    Video video = visible(id);
    List<Long> tagIds = video.getTags().stream().map(Tag::getId).toList();
    if (tagIds.isEmpty()) {
      return new Models.PageResult<>(List.of(), Math.max(page, 1), 12, 0);
    }
    return pageOf(videos.relatedByTags(tagIds, id, pageOf(page, 12, "viewCount")));
  }

  @Transactional(readOnly = true)
  public Models.PageResult<Models.VideoCard> moreByPeople(Long id, int page) {
    Video video = visible(id);
    List<Long> personIds = video.getPeople().stream().map(Person::getId).toList();
    if (personIds.isEmpty()) {
      return new Models.PageResult<>(List.of(), Math.max(page, 1), 12, 0);
    }
    return pageOf(videos.moreByPeople(personIds, id, pageOf(page, 12, "viewCount")));
  }

  @Transactional(readOnly = true)
  public List<Models.VideoCard> byIds(List<Long> ids) {
    if (ids == null || ids.isEmpty()) {
      return List.of();
    }
    Map<Long, Video> loaded = new HashMap<>();
    videos.findByIdIn(ids).stream()
        .filter(video -> video.getStatus() == Vocab.VideoStatus.PUBLISHED)
        .forEach(video -> loaded.put(video.getId(), video));
    return ids.stream().map(loaded::get).filter(v -> v != null).map(this::card).toList();
  }

  @Transactional
  public boolean recordView(Long id, boolean alreadyCounted) {
    if (alreadyCounted) {
      return false;
    }
    Video video = visible(id);
    UserAccount user = CurrentUser.optional();
    if (!access.canPlay(video, user) || video.getStatus() != Vocab.VideoStatus.PUBLISHED) {
      return false;
    }
    video.setViewCount(video.getViewCount() + 1);
    LocalDate today = LocalDate.now(ZoneOffset.UTC);
    VideoViewDaily daily = dailyViews.findByVideoIdAndViewDate(id, today).orElseGet(() -> {
      VideoViewDaily created = new VideoViewDaily();
      created.setVideoId(id);
      created.setViewDate(today);
      created.setViewCount(0);
      return created;
    });
    daily.setViewCount(daily.getViewCount() + 1);
    dailyViews.save(daily);
    return true;
  }

  public Models.VideoCard card(Video video) {
    Series series = video.getSeries();
    return new Models.VideoCard(video.getId(), LocaleTexts.show(video.getI18n(), video.getTitle()), delivery.mediaUrl(video.getCoverKey()),
        video.getDurationMs(), video.getViewCount(), video.getPublishedAt(), video.getAccessType(), video.getPriceUsdt(),
        video.getEpisodeNo(),
        series == null ? null : LocaleTexts.show(series.getI18n(), series.getTitle()),
        series == null ? null : series.getStatus().name(),
        series == null ? null : series.getSlug(),
        video.getStatus().name());
  }

  private Models.SeriesCard seriesCard(Series item) {
    int episodes = (int) videos.countBySeriesIdAndStatus(item.getId(), Vocab.VideoStatus.PUBLISHED);
    return new Models.SeriesCard(item.getId(), LocaleTexts.show(item.getI18n(), item.getTitle()), item.getSlug(), delivery.mediaUrl(item.getCoverKey()),
        item.getStatus().name(), episodes, item.getSourceUrl() == null ? "" : item.getSourceUrl(), LocaleTexts.form(item.getI18n(), item.getTitle()));
  }

  private Models.PageResult<Models.VideoCard> pageOf(Page<Video> page) {
    return new Models.PageResult<>(page.getContent().stream().map(this::card).toList(),
        page.getNumber() + 1, page.getSize(), page.getTotalElements());
  }

  private PageRequest pageOf(int page, int size, String sort) {
    return PageRequest.of(Math.max(page, 1) - 1, size, Sort.by(Sort.Direction.DESC, sort));
  }

  private Video visible(Long id) {
    Video video = videos.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "影片不存在"));
    UserAccount user = CurrentUser.optional();
    if (video.getStatus() != Vocab.VideoStatus.PUBLISHED && !isAdmin(user)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "影片不存在");
    }
    return video;
  }

  private boolean isAdmin(UserAccount user) {
    return user != null && user.getRole() == Vocab.Role.ADMIN;
  }

  private Models.TaxonomyView taxonomy(Category category) {
    return new Models.TaxonomyView(category.getId(), LocaleTexts.show(category.getI18n(), category.getName()), category.getSlug(), category.getSortOrder(),
        category.isShowInNav(), category.isShowOnHome(), LocaleTexts.form(category.getI18n(), category.getName()));
  }

  private Models.TaxonomyView tagTaxonomy(Tag tag) {
    return new Models.TaxonomyView(tag.getId(), LocaleTexts.show(tag.getI18n(), tag.getName()), tag.getSlug(), tag.getSortOrder(), tag.isShowInNav(), tag.isShowOnHome(),
        LocaleTexts.form(tag.getI18n(), tag.getName()));
  }

  private static String text(Object[] row, int index) {
    return row.length > index && row[index] != null ? String.valueOf(row[index]) : "";
  }

  private static long num(Object value) {
    return ((Number) value).longValue();
  }
}
