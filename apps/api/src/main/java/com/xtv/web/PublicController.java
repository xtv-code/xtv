package com.xtv.web;

import com.xtv.domain.UserAccount;
import com.xtv.domain.Vocab;
import com.xtv.dto.Models;
import com.xtv.dto.Requests;
import com.xtv.service.AuthService;
import com.xtv.service.CatalogService;
import com.xtv.service.CurrentUser;
import com.xtv.service.FavoriteService;
import com.xtv.service.OpsService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class PublicController {
  private final CatalogService catalog;
  private final AuthService auth;
  private final OpsService ops;
  private final FavoriteService favorites;

  public PublicController(CatalogService catalog, AuthService auth, OpsService ops, FavoriteService favorites) {
    this.catalog = catalog;
    this.auth = auth;
    this.ops = ops;
    this.favorites = favorites;
  }

  @GetMapping("/site")
  public Models.SiteView site() {
    return ops.site();
  }

  @GetMapping("/legal/{code}")
  public Models.LegalPage legal(@PathVariable String code) {
    return ops.legal(code);
  }

  @GetMapping("/nav")
  public Models.NavResponse nav() {
    return catalog.nav();
  }

  @GetMapping("/home")
  public Models.HomeResponse home() {
    return catalog.home();
  }

  @GetMapping("/videos")
  public Models.PageResult<Models.VideoCard> latest(@RequestParam(defaultValue = "1") int page) {
    return catalog.latest(page);
  }

  @GetMapping("/videos/{id}")
  public Models.VideoDetail video(@PathVariable Long id) {
    return catalog.detail(id);
  }

  @GetMapping("/videos/{id}/play")
  public Models.PlayResult play(@PathVariable Long id) {
    return catalog.play(id);
  }

  @GetMapping("/videos/{id}/related")
  public Models.PageResult<Models.VideoCard> related(@PathVariable Long id, @RequestParam(defaultValue = "1") int page) {
    return catalog.related(id, page);
  }

  @GetMapping("/videos/{id}/same-people")
  public Models.PageResult<Models.VideoCard> samePeople(@PathVariable Long id, @RequestParam(defaultValue = "1") int page) {
    return catalog.moreByPeople(id, page);
  }

  @PostMapping("/videos/{id}/views")
  public void view(@PathVariable Long id, HttpServletRequest request, HttpServletResponse response) {
    String seen = null;
    if (request.getCookies() != null) {
      for (Cookie cookie : request.getCookies()) {
        if (("xtv_view_" + id).equals(cookie.getName())) {
          seen = cookie.getValue();
        }
      }
    }
    boolean counted = catalog.recordView(id, "1".equals(seen));
    if (counted) {
      Cookie cookie = new Cookie("xtv_view_" + id, "1");
      cookie.setPath("/");
      cookie.setMaxAge(24 * 3600);
      response.addCookie(cookie);
    }
  }

  @PostMapping("/videos/by-ids")
  public List<Models.VideoCard> byIds(@RequestBody Requests.IdsRequest req) {
    return catalog.byIds(req.ids());
  }

  @GetMapping("/categories")
  public Models.PageResult<Models.CategoryCard> categories(@RequestParam(defaultValue = "1") int page) {
    return catalog.categoryPage(page);
  }

  @GetMapping("/categories/{slug}/videos")
  public Models.PageResult<Models.VideoCard> categoryVideos(@PathVariable String slug, @RequestParam(defaultValue = "1") int page) {
    return catalog.byCategory(slug, page);
  }

  @GetMapping("/tags")
  public Models.PageResult<Models.TagCard> tags(@RequestParam(defaultValue = "1") int page) {
    return catalog.tags(page);
  }

  @GetMapping("/tags/{slug}/videos")
  public Models.PageResult<Models.VideoCard> tagVideos(@PathVariable String slug, @RequestParam(defaultValue = "1") int page) {
    return catalog.byTag(slug, page);
  }

  @GetMapping("/people")
  public Models.PageResult<Models.PersonCard> people(@RequestParam(defaultValue = "PERFORMER") String kind,
                                                     @RequestParam(defaultValue = "1") int page,
                                                     @RequestParam(defaultValue = "12") int size) {
    return catalog.people(Vocab.PersonKind.valueOf(kind), page, Math.min(size, 200));
  }

  @GetMapping("/people/{slug}")
  public Models.PageResult<Models.VideoCard> person(@PathVariable String slug, @RequestParam(defaultValue = "1") int page) {
    return catalog.byPerson(slug, page);
  }

  @GetMapping("/series")
  public Models.PageResult<Models.SeriesCard> series(@RequestParam(defaultValue = "1") int page) {
    return catalog.seriesList(page);
  }

  @GetMapping("/series/{slug}")
  public Models.SeriesDetail seriesDetail(@PathVariable String slug, @RequestParam(defaultValue = "1") int page) {
    return catalog.series(slug, page);
  }

  @GetMapping("/ranking")
  public Models.PageResult<Models.VideoCard> ranking(@RequestParam(defaultValue = "all") String range,
                                                     @RequestParam(defaultValue = "1") int page) {
    return catalog.ranking(range, page);
  }

  @GetMapping("/search")
  public Models.PageResult<Models.VideoCard> search(@RequestParam(defaultValue = "") String q,
                                                    @RequestParam(defaultValue = "1") int page) {
    return catalog.search(q, page);
  }

  @GetMapping("/ads")
  public Models.AdView ad(@RequestParam String slot) {
    Vocab.AdSlot parsed;
    try {
      parsed = Vocab.AdSlot.valueOf(slot);
    } catch (Exception ex) {
      throw new org.springframework.web.server.ResponseStatusException(
          org.springframework.http.HttpStatus.BAD_REQUEST, "广告位置无效");
    }
    return ops.active(parsed);
  }

  @GetMapping("/membership-plans")
  public List<Models.PlanView> plans() {
    return ops.plans(true);
  }

  @PostMapping("/auth/register")
  public Models.AuthResponse register(@Valid @RequestBody Requests.RegisterRequest req) {
    return auth.register(req);
  }

  @PostMapping("/auth/login")
  public Models.AuthResponse login(@Valid @RequestBody Requests.LoginRequest req) {
    return auth.login(req);
  }

  @GetMapping("/me")
  public Models.MeResponse me() {
    return auth.me(CurrentUser.require());
  }

  @PutMapping("/me/password")
  public void changePassword(@Valid @RequestBody Requests.PasswordChangeRequest req) {
    auth.changePassword(CurrentUser.require(), req);
  }

  @GetMapping("/me/favorites")
  public List<Models.VideoCard> favorites() {
    return favorites.list(CurrentUser.require());
  }

  @GetMapping("/me/favorites/{videoId}")
  public Models.FavoriteState favorite(@PathVariable Long videoId) {
    return new Models.FavoriteState(favorites.saved(CurrentUser.require(), videoId));
  }

  @PutMapping("/me/favorites/{videoId}")
  public Models.FavoriteState toggleFavorite(@PathVariable Long videoId) {
    return new Models.FavoriteState(favorites.toggle(CurrentUser.require(), videoId));
  }

  @PostMapping("/me/favorites/sync")
  public void syncFavorites(@RequestBody Requests.IdsRequest req) {
    favorites.sync(CurrentUser.require(), req.ids());
  }

  @PostMapping("/reports")
  public Models.ReportView report(@Valid @RequestBody Requests.ReportRequest req) {
    return ops.report(req);
  }
}
