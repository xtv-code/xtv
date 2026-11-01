package com.xtv.config;

import com.xtv.domain.Category;
import com.xtv.domain.MembershipPlan;
import com.xtv.domain.Person;
import com.xtv.domain.PlatformConfig;
import com.xtv.domain.Series;
import com.xtv.domain.Tag;
import com.xtv.domain.Video;
import com.xtv.domain.VideoViewDaily;
import com.xtv.domain.Vocab;
import com.xtv.repo.CategoryRepository;
import com.xtv.repo.MembershipPlanRepository;
import com.xtv.repo.PersonRepository;
import com.xtv.repo.PlatformConfigRepository;
import com.xtv.repo.SeriesRepository;
import com.xtv.repo.TagRepository;
import com.xtv.repo.VideoRepository;
import com.xtv.repo.VideoViewDailyRepository;
import com.xtv.service.MediaStore;
import com.xtv.service.PosterArt;
import com.xtv.service.Slugs;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(30)
public class CatalogSeed implements ApplicationRunner {
  private static final List<Clip> CLIPS = List.of(
      clip("晚风里的那封信", "都市", null, null, "FREE", null, 186420, 28, 1, true, "都市,热门", "林晚宁,周予安,何清越"),
      clip("离职后他追到了我家", "都市", null, null, "FREE", null, 94210, 22, 4, false, "都市,职场", "苏小满,顾承衍"),
      clip("合约到期那天", "都市", null, null, "MEMBER", null, 43120, 36, 8, false, "都市,豪门", "顾承衍,林晚宁"),
      clip("雨夜出租屋", "都市", null, null, "FREE", null, 22840, 19, 12, false, "都市,悬疑", "陈予白,赵闻川"),
      clip("隔壁哥哥是我的", "甜宠", null, null, "FREE", null, 256880, 16, 2, true, "甜宠,热门", "沈清禾,周予安"),
      clip("婚礼上的陌生人", "甜宠", "先婚后爱进行时", 1, "FREE", null, 210440, 3, 2, false, "甜宠,豪门", "林晚宁,顾承衍,何清越"),
      clip("同居第一夜", "甜宠", "先婚后爱进行时", 2, "FREE", null, 188200, 3, 3, false, "甜宠,豪门", "林晚宁,顾承衍"),
      clip("旧人来电", "甜宠", "先婚后爱进行时", 3, "FREE", null, 166900, 4, 5, false, "甜宠,豪门", "林晚宁,顾承衍,苏小满"),
      clip("公开关系", "甜宠", "先婚后爱进行时", 4, "MEMBER", null, 142330, 3, 6, false, "甜宠,豪门", "林晚宁,顾承衍"),
      clip("他的秘密", "甜宠", "先婚后爱进行时", 5, "FREE", null, 121050, 4, 9, false, "甜宠,豪门", "林晚宁,顾承衍"),
      clip("未完的誓言", "甜宠", "先婚后爱进行时", 6, "FREE", null, 98040, 5, 11, false, "甜宠,豪门,热门", "林晚宁,顾承衍"),
      clip("女帝她又摆烂了", "古装", null, null, "FREE", null, 301220, 24, 1, true, "古装,热门", "沈清禾,江宴"),
      clip("王府里的小厨娘", "古装", null, null, "FREE", null, 87440, 21, 6, false, "古装", "苏小满,江宴,赵闻川"),
      clip("反派的白月光", "古装", null, null, "MEMBER", null, 65330, 27, 10, false, "古装,重生", "沈清禾,周予安"),
      clip("宫墙外的灯", "古装", null, null, "FREE", null, 41220, 18, 15, false, "古装", "陈予白,江宴"),
      clip("进港", "悬疑", "雾港回声", 1, "FREE", null, 156770, 8, 3, false, "悬疑,热门", "陈予白,周予安,何清越"),
      clip("第三位客人", "悬疑", "雾港回声", 2, "FREE", null, 132410, 8, 4, false, "悬疑", "陈予白,周予安"),
      clip("雨夜", "悬疑", "雾港回声", 3, "FREE", null, 118900, 7, 7, false, "悬疑", "陈予白,林晚宁"),
      clip("失踪名单", "悬疑", "雾港回声", 4, "FREE", null, 99020, 9, 8, false, "悬疑", "陈予白,周予安"),
      clip("灯塔", "悬疑", "雾港回声", 5, "MEMBER", null, 86440, 8, 13, false, "悬疑", "陈予白,周予安"),
      clip("回声", "悬疑", "雾港回声", 6, "PPV", "1.90", 74210, 10, 16, false, "悬疑,热门", "陈予白,周予安"),
      clip("被退婚后我闪婚了首富", "逆袭", null, null, "PPV", "2.90", 410560, 32, 1, true, "逆袭,豪门,热门", "苏小满,顾承衍"),
      clip("重生回到高考前", "逆袭", null, null, "MEMBER", null, 173220, 26, 5, false, "逆袭,重生", "沈清禾,何清越"),
      clip("离职第三年", "逆袭", null, null, "FREE", null, 68990, 20, 14, false, "逆袭,职场", "周予安,苏小满"),
      clip("她从小镇回来了", "逆袭", null, null, "FREE", null, 53440, 23, 18, false, "逆袭", "林晚宁,赵闻川"),
      clip("妈妈的小餐馆", "家庭", null, null, "FREE", null, 120340, 17, 3, false, "家庭,热门", "苏小满,陈予白"),
      clip("回家的那班高铁", "家庭", null, null, "FREE", null, 88610, 15, 7, false, "家庭", "周予安,林晚宁"),
      clip("父亲的旧相机", "家庭", null, null, "FREE", null, 45120, 29, 20, false, "家庭", "江宴,赵闻川"),
      clip("周末只做一顿饭", "家庭", null, null, "FREE", null, 33880, 14, 22, false, "家庭", "沈清禾,苏小满")
  );

  private final XtvProperties properties;
  private final PlatformConfigRepository configs;
  private final CategoryRepository categories;
  private final TagRepository tags;
  private final PersonRepository people;
  private final SeriesRepository seriesRepo;
  private final VideoRepository videos;
  private final VideoViewDailyRepository dailyViews;
  private final MembershipPlanRepository plans;
  private final MediaStore media;

  public CatalogSeed(XtvProperties properties, PlatformConfigRepository configs, CategoryRepository categories,
                     TagRepository tags, PersonRepository people, SeriesRepository seriesRepo, VideoRepository videos,
                     VideoViewDailyRepository dailyViews, MembershipPlanRepository plans, MediaStore media) {
    this.properties = properties;
    this.configs = configs;
    this.categories = categories;
    this.tags = tags;
    this.people = people;
    this.seriesRepo = seriesRepo;
    this.videos = videos;
    this.dailyViews = dailyViews;
    this.plans = plans;
    this.media = media;
  }

  @Override
  @Transactional
  public void run(ApplicationArguments args) throws Exception {
    if (!properties.isCatalogSeed()) {
      return;
    }
    PlatformConfig cfg = configs.findById(1L).orElseThrow();
    if (cfg.isCatalogSeeded()) {
      return;
    }
    rename("测试短片", "晚风里的那封信");
    rename("测试2", "雨夜出租屋");
    renameSeries("短剧测试", "雾港回声", Vocab.SeriesStatus.COMPLETED);
    for (String[] row : new String[][] {
        {"都市", "1"}, {"甜宠", "2"}, {"古装", "3"}, {"悬疑", "4"}, {"逆袭", "5"}, {"家庭", "6"}
    }) {
      category(row[0], Integer.parseInt(row[1]));
    }
    category("剧情", 7);
    for (String name : List.of("都市", "甜宠", "古装", "悬疑", "逆袭", "家庭", "豪门", "职场", "重生", "热门")) {
      tag(name);
    }
    for (String[] row : new String[][] {
        {"林晚宁", "PERFORMER"}, {"周予安", "PERFORMER"}, {"苏小满", "PERFORMER"}, {"顾承衍", "PERFORMER"},
        {"沈清禾", "PERFORMER"}, {"江宴", "PERFORMER"}, {"陈予白", "PERFORMER"}, {"何清越", "CREATOR"}, {"赵闻川", "CREATOR"}
    }) {
      person(row[0], Vocab.PersonKind.valueOf(row[1]));
    }
    series("先婚后爱进行时", Vocab.SeriesStatus.ONGOING);
    series("雾港回声", Vocab.SeriesStatus.COMPLETED);
    plan("季度会员", 90, "26");
    plan("年度会员", 365, "88");
    int n = 0;
    for (Clip clip : CLIPS) {
      place(clip, n++);
    }
    cfg.setCatalogSeeded(true);
    configs.save(cfg);
  }

  private void rename(String from, String to) {
    videos.findFirstByTitle(from).ifPresent(video -> {
      if (videos.findFirstByTitle(to).isEmpty()) {
        video.setTitle(to);
        videos.save(video);
      }
    });
  }

  private void renameSeries(String fromSlug, String title, Vocab.SeriesStatus status) {
    seriesRepo.findBySlug(fromSlug).ifPresent(item -> {
      item.setTitle(title);
      item.setSlug(Slugs.unique(title, slug -> !slug.equals(item.getSlug()) && seriesRepo.existsBySlug(slug)));
      item.setStatus(status);
      seriesRepo.save(item);
    });
  }

  private void place(Clip clip, int index) throws Exception {
    Video video = videos.findFirstByTitle(clip.title).orElseGet(Video::new);
    boolean fresh = video.getId() == null;
    video.setTitle(clip.title);
    if (fresh) {
      video.setCreatedAt(Instant.now());
      video.setStatus(Vocab.VideoStatus.PUBLISHED);
    } else if (video.getStatus() == Vocab.VideoStatus.DRAFT) {
      video.setStatus(Vocab.VideoStatus.PUBLISHED);
    }
    if (video.getDurationMs() == null || video.getDurationMs() < 1000) {
      video.setDurationMs(clip.minutes * 60_000L);
    }
    if (video.getViewCount() < 1000) {
      video.setViewCount(clip.views);
    }
    if (video.getPublishedAt() == null) {
      video.setPublishedAt(Instant.now().minus(clip.daysAgo, ChronoUnit.DAYS));
    }
    if (fresh || video.getAccessType() == null) {
      video.setAccessType(Vocab.AccessType.valueOf(clip.access));
      video.setPriceUsdt(clip.price == null ? null : new BigDecimal(clip.price));
    }
    video.setFeatured(video.isFeatured() || clip.featured);
    if (clip.series != null) {
      video.setSeries(series(clip.series, clip.series.contains("雾港") ? Vocab.SeriesStatus.COMPLETED : Vocab.SeriesStatus.ONGOING));
      video.setEpisodeNo(clip.episode);
    }
    video.getCategories().clear();
    video.getCategories().add(category(clip.category, 0));
    if ("晚风里的那封信".equals(clip.title) || "雨夜出租屋".equals(clip.title) || "进港".equals(clip.title) || "父亲的旧相机".equals(clip.title)) {
      video.getCategories().add(category("剧情", 7));
    }
    video.getTags().clear();
    for (String name : clip.tags) {
      video.getTags().add(tag(name));
    }
    video.getPeople().clear();
    for (String name : clip.people) {
      video.getPeople().add(person(name, name.equals("何清越") || name.equals("赵闻川") ? Vocab.PersonKind.CREATOR : Vocab.PersonKind.PERFORMER));
    }
    video = videos.save(video);
    String key = "covers/" + video.getId() + ".jpg";
    int[] tone = tone(index);
    PosterArt.poster(media.publicFile(key), clip.title, clip.series == null ? clip.category : clip.series, tone[0], tone[1]);
    video.setCoverKey(key);
    videos.save(video);
    seedViews(video.getId(), clip.views);
  }

  private void seedViews(Long videoId, long views) {
    long recent = Math.max(120, views / 16);
    LocalDate today = LocalDate.now();
    for (int day = 0; day < 14; day++) {
      LocalDate date = today.minusDays(day);
      if (dailyViews.findByVideoIdAndViewDate(videoId, date).isPresent()) {
        continue;
      }
      VideoViewDaily row = new VideoViewDaily();
      row.setVideoId(videoId);
      row.setViewDate(date);
      row.setViewCount(Math.max(40, recent / (1 + day % 4)));
      dailyViews.save(row);
    }
  }

  private Category category(String name, int sort) {
    return categories.findBySlug(Slugs.of(name)).orElseGet(() -> {
      Category category = new Category();
      category.setName(name);
      category.setSlug(Slugs.unique(name, categories::existsBySlug));
      category.setSortOrder(sort == 0 ? 20 : sort);
      category.setShowInNav(true);
      category.setShowOnHome(true);
      return categories.save(category);
    });
  }

  private Tag tag(String name) {
    return tags.findBySlug(Slugs.of(name)).orElseGet(() -> {
      Tag tag = new Tag();
      tag.setName(name);
      tag.setSlug(Slugs.unique(name, tags::existsBySlug));
      tag.setSortOrder(10);
      tag.setShowInNav(false);
      tag.setShowOnHome(false);
      return tags.save(tag);
    });
  }

  private Person person(String name, Vocab.PersonKind kind) throws Exception {
    Person person = people.findBySlug(Slugs.of(name)).orElseGet(() -> {
      Person created = new Person();
      created.setName(name);
      created.setKind(kind);
      created.setSlug(Slugs.unique(name, people::existsBySlug));
      return people.save(created);
    });
    if (person.getAvatarKey() == null || person.getAvatarKey().isBlank()) {
      String key = "avatars/" + person.getId() + ".jpg";
      int[] tone = tone(name.hashCode());
      PosterArt.avatar(media.publicFile(key), name, tone[0], tone[1]);
      person.setAvatarKey(key);
      person = people.save(person);
    }
    return person;
  }

  private Series series(String title, Vocab.SeriesStatus status) throws Exception {
    Series series = seriesRepo.findBySlug(Slugs.of(title)).orElseGet(() -> {
      Series created = new Series();
      created.setTitle(title);
      created.setStatus(status);
      created.setSlug(Slugs.unique(title, seriesRepo::existsBySlug));
      return seriesRepo.save(created);
    });
    series.setStatus(status);
    if (series.getCoverKey() == null || series.getCoverKey().isBlank()) {
      String key = "covers/series-" + series.getId() + ".jpg";
      int[] tone = tone(title.hashCode());
      PosterArt.poster(media.publicFile(key), title, status == Vocab.SeriesStatus.COMPLETED ? "已完结" : "更新中", tone[0], tone[1]);
      series.setCoverKey(key);
    }
    return seriesRepo.save(series);
  }

  private void plan(String name, int days, String price) {
    boolean exists = plans.findAll().stream().anyMatch(item -> name.equals(item.getName()));
    if (exists) {
      return;
    }
    MembershipPlan plan = new MembershipPlan();
    plan.setName(name);
    plan.setDurationDays(days);
    plan.setPriceUsdt(new BigDecimal(price));
    plan.setEnabled(true);
    plans.save(plan);
  }

  private static int[] tone(int seed) {
    int[][] tones = {
        {0x1c2430, 0xc45c26},
        {0x241820, 0x8d3a55},
        {0x14241c, 0x2f6b4f},
        {0x1a1c2e, 0x3d5a99},
        {0x2a2116, 0xb5812f},
        {0x221828, 0x6a4c93}
    };
    int index = Math.floorMod(seed, tones.length);
    return tones[index];
  }

  private static Clip clip(String title, String category, String series, Integer episode, String access, String price,
                           long views, int minutes, int daysAgo, boolean featured, String tags, String people) {
    return new Clip(title, category, series, episode, access, price, views, minutes, daysAgo, featured,
        tags.split(","), people.split(","));
  }

  private record Clip(String title, String category, String series, Integer episode, String access, String price,
                      long views, int minutes, int daysAgo, boolean featured, String[] tags, String[] people) {}
}
