package com.xtv.web;

import com.xtv.billing.BillingService;
import com.xtv.dto.Models;
import com.xtv.dto.Requests;
import com.xtv.service.AuthService;
import com.xtv.service.CatalogService;
import com.xtv.service.ImportService;
import com.xtv.service.OpsService;
import com.xtv.service.TaxonomyService;
import com.xtv.service.VideoAdminService;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {
  private final VideoAdminService videos;
  private final TaxonomyService taxonomy;
  private final OpsService ops;
  private final BillingService billing;
  private final CatalogService catalog;
  private final AuthService auth;
  private final ImportService imports;

  public AdminController(VideoAdminService videos, TaxonomyService taxonomy, OpsService ops, BillingService billing,
                         CatalogService catalog, AuthService auth, ImportService imports) {
    this.videos = videos;
    this.taxonomy = taxonomy;
    this.ops = ops;
    this.billing = billing;
    this.catalog = catalog;
    this.auth = auth;
    this.imports = imports;
  }

  @GetMapping("/videos")
  public Models.PageResult<Models.VideoCard> videos(@RequestParam(defaultValue = "1") int page,
                                                    @RequestParam(defaultValue = "12") int size,
                                                    @RequestParam(required = false) String title,
                                                    @RequestParam(required = false) String accessType,
                                                    @RequestParam(required = false) String status,
                                                    @RequestParam(defaultValue = "id") String sort,
                                                    @RequestParam(defaultValue = "desc") String dir) {
    return videos.list(page, size, title, accessType, status, sort, dir);
  }

  @PostMapping("/videos/{id}/cover-url")
  public Models.VideoDetail videoCover(@PathVariable Long id, @Valid @RequestBody Requests.CoverUrlRequest req) {
    return imports.saveVideoCover(id, req.url());
  }

  @PostMapping(value = "/videos/{id}/cover", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public Models.VideoDetail videoCoverFile(@PathVariable Long id, @RequestPart("file") MultipartFile file) {
    return imports.saveVideoCoverFile(id, file);
  }

  @PostMapping("/videos")
  public Models.VideoDetail create(@Valid @RequestBody Requests.VideoCreate req) {
    return videos.create(req);
  }

  @PutMapping("/videos/{id}")
  public Models.VideoDetail update(@PathVariable Long id, @RequestBody Requests.VideoUpdate req) {
    return videos.update(id, req);
  }

  @PutMapping("/videos/{id}/publish")
  public Models.VideoDetail publish(@PathVariable Long id, @RequestParam(defaultValue = "true") boolean published) {
    return videos.publish(id, published);
  }

  @DeleteMapping("/videos/{id}")
  public void deleteVideo(@PathVariable Long id) {
    videos.delete(id);
  }

  @PostMapping("/videos/{id}/upload-credential")
  public Models.UploadTicket ticket(@PathVariable Long id) {
    return videos.uploadTicket(id);
  }

  @PostMapping(value = "/videos/{id}/source", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public Models.JobView source(@PathVariable Long id, @RequestPart("file") MultipartFile file) throws Exception {
    return videos.saveLocalSource(id, file);
  }

  @PostMapping("/videos/{id}/complete-upload")
  public Models.JobView complete(@PathVariable Long id, @RequestParam(defaultValue = "0") long size) {
    return videos.completeObjectUpload(id, size);
  }

  @GetMapping("/videos/{id}/jobs")
  public List<Models.JobView> jobs(@PathVariable Long id) {
    return videos.jobs(id);
  }

  @GetMapping("/categories")
  public Models.PageResult<Models.TaxonomyView> categories(@RequestParam(defaultValue = "1") int page,
                                                           @RequestParam(defaultValue = "12") int size,
                                                           @RequestParam(required = false) String name,
                                                           @RequestParam(required = false) String showInNav,
                                                           @RequestParam(required = false) String showOnHome,
                                                           @RequestParam(required = false) String sortOrder,
                                                           @RequestParam(defaultValue = "sortOrder") String sort,
                                                           @RequestParam(defaultValue = "asc") String dir) {
    return catalog.categoryList(page, size, name, showInNav, showOnHome, sortOrder, sort, dir);
  }

  @GetMapping("/tags")
  public Models.PageResult<Models.TaxonomyView> tags(@RequestParam(defaultValue = "1") int page,
                                                     @RequestParam(defaultValue = "12") int size,
                                                     @RequestParam(required = false) String name,
                                                     @RequestParam(required = false) String sortOrder,
                                                     @RequestParam(defaultValue = "sortOrder") String sort,
                                                     @RequestParam(defaultValue = "asc") String dir) {
    return catalog.tagList(page, size, name, sortOrder, sort, dir);
  }

  @PostMapping("/categories")
  public Models.TaxonomyView createCategory(@RequestBody Requests.TaxonomyRequest req) {
    return taxonomy.saveCategory(null, req);
  }

  @PutMapping("/categories/{id}")
  public Models.TaxonomyView updateCategory(@PathVariable Long id, @RequestBody Requests.TaxonomyRequest req) {
    return taxonomy.saveCategory(id, req);
  }

  @DeleteMapping("/categories/{id}")
  public void deleteCategory(@PathVariable Long id) {
    taxonomy.deleteCategory(id);
  }

  @PostMapping("/tags")
  public Models.TaxonomyView createTag(@RequestBody Requests.TaxonomyRequest req) {
    return taxonomy.saveTag(null, req);
  }

  @PutMapping("/tags/{id}")
  public Models.TaxonomyView updateTag(@PathVariable Long id, @RequestBody Requests.TaxonomyRequest req) {
    return taxonomy.saveTag(id, req);
  }

  @DeleteMapping("/tags/{id}")
  public void deleteTag(@PathVariable Long id) {
    taxonomy.deleteTag(id);
  }

  @GetMapping("/people")
  public Models.PageResult<Models.PersonRef> people(@RequestParam(defaultValue = "1") int page,
                                                    @RequestParam(defaultValue = "12") int size,
                                                    @RequestParam(required = false) String name,
                                                    @RequestParam(required = false) String kind,
                                                    @RequestParam(defaultValue = "name") String sort,
                                                    @RequestParam(defaultValue = "asc") String dir) {
    return taxonomy.listPeople(page, size, name, kind, sort, dir);
  }

  @PostMapping("/people")
  public Models.PersonRef createPerson(@Valid @RequestBody Requests.PersonRequest req) {
    return taxonomy.savePerson(null, req);
  }

  @PutMapping("/people/{id}")
  public Models.PersonRef updatePerson(@PathVariable Long id, @Valid @RequestBody Requests.PersonRequest req) {
    return taxonomy.savePerson(id, req);
  }

  @PostMapping(value = "/people/{id}/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public Models.PersonRef avatar(@PathVariable Long id, @RequestPart("file") MultipartFile file) throws Exception {
    return taxonomy.saveAvatar(id, file);
  }

  @DeleteMapping("/people/{id}")
  public void deletePerson(@PathVariable Long id) {
    taxonomy.deletePerson(id);
  }

  @GetMapping("/series")
  public Models.PageResult<Models.SeriesCard> series(@RequestParam(defaultValue = "1") int page,
                                                     @RequestParam(defaultValue = "12") int size,
                                                     @RequestParam(required = false) String title,
                                                     @RequestParam(required = false) String status,
                                                     @RequestParam(required = false) String episodes,
                                                     @RequestParam(defaultValue = "id") String sort,
                                                     @RequestParam(defaultValue = "desc") String dir) {
    return taxonomy.listSeries(page, size, title, status, episodes, sort, dir);
  }

  @PostMapping("/series/{id}/cover-url")
  public Models.SeriesCard seriesCoverUrl(@PathVariable Long id, @Valid @RequestBody Requests.CoverUrlRequest req) {
    return imports.saveSeriesCover(id, req.url());
  }

  @PostMapping(value = "/series/{id}/cover", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public Models.SeriesCard seriesCover(@PathVariable Long id, @RequestPart("file") MultipartFile file) throws Exception {
    return taxonomy.saveCover(id, file);
  }

  @PostMapping("/series")
  public Models.SeriesCard createSeries(@Valid @RequestBody Requests.SeriesRequest req) {
    return taxonomy.saveSeries(null, req);
  }

  @PutMapping("/series/{id}")
  public Models.SeriesCard updateSeries(@PathVariable Long id, @Valid @RequestBody Requests.SeriesRequest req) {
    return taxonomy.saveSeries(id, req);
  }

  @DeleteMapping("/series/{id}")
  public void deleteSeries(@PathVariable Long id) {
    taxonomy.deleteSeries(id);
  }

  @GetMapping("/plans")
  public List<Models.PlanView> plans(@RequestParam(required = false) String name,
                                     @RequestParam(required = false) String enabled,
                                     @RequestParam(required = false) String durationDays,
                                     @RequestParam(required = false) String priceUsdt,
                                     @RequestParam(defaultValue = "id") String sort,
                                     @RequestParam(defaultValue = "desc") String dir) {
    return ops.plans(false, name, enabled, durationDays, priceUsdt, sort, dir);
  }

  @PostMapping("/plans")
  public Models.PlanView createPlan(@RequestBody Requests.PlanRequest req) {
    return ops.savePlan(null, req);
  }

  @PutMapping("/plans/{id}")
  public Models.PlanView updatePlan(@PathVariable Long id, @RequestBody Requests.PlanRequest req) {
    return ops.savePlan(id, req);
  }

  @DeleteMapping("/plans/{id}")
  public void deletePlan(@PathVariable Long id) {
    ops.deletePlan(id);
  }

  @GetMapping("/ads")
  public List<Models.AdView> ads(@RequestParam(required = false) String slot,
                                 @RequestParam(required = false) String enabled,
                                 @RequestParam(required = false) String targetUrl,
                                 @RequestParam(defaultValue = "id") String sort,
                                 @RequestParam(defaultValue = "desc") String dir) {
    return ops.allAds(slot, enabled, targetUrl, sort, dir);
  }

  @PostMapping(value = "/ads", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public Models.AdView createAd(@RequestParam String slot, @RequestParam String targetUrl,
                                @RequestParam(defaultValue = "true") boolean enabled,
                                @RequestParam(required = false) Instant startAt,
                                @RequestParam(required = false) Instant endAt,
                                @RequestParam(defaultValue = "0") int sortOrder,
                                @RequestPart("file") MultipartFile file) throws Exception {
    return ops.saveAd(null, slot, targetUrl, enabled, startAt, endAt, sortOrder, file);
  }

  @PutMapping(value = "/ads/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public Models.AdView updateAd(@PathVariable Long id, @RequestParam String slot, @RequestParam String targetUrl,
                                @RequestParam(defaultValue = "true") boolean enabled,
                                @RequestParam(required = false) Instant startAt,
                                @RequestParam(required = false) Instant endAt,
                                @RequestParam(defaultValue = "0") int sortOrder,
                                @RequestPart(value = "file", required = false) MultipartFile file) throws Exception {
    return ops.saveAd(id, slot, targetUrl, enabled, startAt, endAt, sortOrder, file);
  }

  @DeleteMapping("/ads/{id}")
  public void deleteAd(@PathVariable Long id) {
    ops.deleteAd(id);
  }

  @GetMapping("/orders")
  public Models.PageResult<Models.OrderView> orders(@RequestParam(defaultValue = "1") int page,
                                                    @RequestParam(defaultValue = "12") int size,
                                                    @RequestParam(required = false) String id,
                                                    @RequestParam(required = false) String orderType,
                                                    @RequestParam(required = false) String state,
                                                    @RequestParam(required = false) String payAmount,
                                                    @RequestParam(required = false) String createdAt,
                                                    @RequestParam(defaultValue = "createdAt") String sort,
                                                    @RequestParam(defaultValue = "desc") String dir) {
    return billing.all(page, size, id, orderType, state, payAmount, createdAt, sort, dir);
  }

  @PostMapping("/orders/{id}/confirm")
  public Models.OrderView confirmOrder(@PathVariable Long id) {
    return billing.adminConfirm(id);
  }

  @PostMapping("/orders/{id}/cancel")
  public Models.OrderView cancelOrder(@PathVariable Long id) {
    return billing.cancel(id);
  }

  @GetMapping("/users")
  public Models.PageResult<Models.UserView> users(@RequestParam(defaultValue = "1") int page,
                                                  @RequestParam(defaultValue = "12") int size,
                                                  @RequestParam(required = false) String username,
                                                  @RequestParam(required = false) String role,
                                                  @RequestParam(required = false) String memberUntil,
                                                  @RequestParam(defaultValue = "id") String sort,
                                                  @RequestParam(defaultValue = "desc") String dir) {
    return auth.users(page, size, username, role, memberUntil, sort, dir);
  }

  @PutMapping("/users/{id}/password")
  public void resetPassword(@PathVariable Long id, @Valid @RequestBody Requests.PasswordResetRequest req) {
    auth.resetPassword(id, req);
  }

  @PostMapping("/users")
  public Models.UserView createUser(@Valid @RequestBody Requests.AdminUserCreate req) {
    return auth.createUser(req);
  }

  @PutMapping("/users/{id}")
  public Models.UserView updateUser(@PathVariable Long id, @RequestBody Requests.AdminUserUpdate req) {
    return auth.updateUser(id, req);
  }

  @DeleteMapping("/users/{id}")
  public void deleteUser(@PathVariable Long id) {
    auth.deleteUser(id);
  }

  @GetMapping("/reports")
  public Models.PageResult<Models.ReportView> reports(@RequestParam(defaultValue = "1") int page,
                                                      @RequestParam(defaultValue = "12") int size,
                                                      @RequestParam(required = false) String contact,
                                                      @RequestParam(required = false) String videoId,
                                                      @RequestParam(required = false) String status,
                                                      @RequestParam(required = false) String createdAt,
                                                      @RequestParam(required = false) String message,
                                                      @RequestParam(defaultValue = "id") String sort,
                                                      @RequestParam(defaultValue = "desc") String dir) {
    return ops.reports(page, size, contact, videoId, status, createdAt, message, sort, dir);
  }

  @PutMapping("/reports/{id}/handle")
  public Models.ReportView handleReport(@PathVariable Long id) {
    return ops.handleReport(id);
  }

  @PutMapping("/reports/{id}/reopen")
  public Models.ReportView reopenReport(@PathVariable Long id) {
    return ops.reopenReport(id);
  }

  @DeleteMapping("/reports/{id}")
  public void deleteReport(@PathVariable Long id) {
    ops.deleteReport(id);
  }

  @GetMapping("/platform-config")
  public Models.PlatformView platform() {
    return ops.platform();
  }

  @PutMapping("/platform-config")
  public Models.PlatformView savePlatform(@RequestBody Requests.PlatformRequest req) {
    return ops.savePlatform(req);
  }

  @GetMapping("/chain-receivers")
  public java.util.List<Models.ChainReceiverView> chainReceivers() {
    return billing.chainReceivers();
  }

  @PutMapping("/chain-receivers")
  public java.util.List<Models.ChainReceiverView> saveChainReceivers(@RequestBody java.util.List<Requests.ChainReceiverRequest> req) {
    return billing.saveChainReceivers(req);
  }

  @GetMapping("/legal")
  public Models.LegalEditor legal() {
    return ops.legalEditor();
  }

  @PutMapping("/legal")
  public Models.LegalEditor saveLegal(@RequestBody Requests.LegalWrite req) {
    return ops.saveLegal(req);
  }
}
