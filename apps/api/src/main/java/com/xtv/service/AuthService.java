package com.xtv.service;

import com.xtv.config.XtvProperties;
import com.xtv.domain.UserAccount;
import com.xtv.domain.Vocab;
import com.xtv.dto.Models;
import com.xtv.dto.Requests;
import com.xtv.repo.UserAccountRepository;
import com.xtv.repo.UserFavoriteRepository;
import com.xtv.repo.VideoEntitlementRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import javax.crypto.SecretKey;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {
  private final UserAccountRepository users;
  private final VideoEntitlementRepository entitlements;
  private final UserFavoriteRepository favorites;
  private final PasswordEncoder encoder;
  private final XtvProperties properties;

  public AuthService(UserAccountRepository users, VideoEntitlementRepository entitlements, UserFavoriteRepository favorites,
                     PasswordEncoder encoder, XtvProperties properties) {
    this.users = users;
    this.entitlements = entitlements;
    this.favorites = favorites;
    this.encoder = encoder;
    this.properties = properties;
  }

  public Models.AuthResponse register(Requests.RegisterRequest req) {
    if (users.existsByUsername(req.username().trim())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "用户名已存在");
    }
    return token(users.save(newUser(req.username().trim(), req.password())));
  }

  public Models.AuthResponse login(Requests.LoginRequest req) {
    String username = req.username().trim();
    String password = req.password();
    return users.findByUsername(username)
        .map(user -> {
          if (!encoder.matches(password, user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "密码错误");
          }
          return token(user);
        })
        .orElseGet(() -> {
          if (!properties.isAutoRegister()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "账号不存在或密码错误");
          }
          return token(users.save(newUser(username, password)));
        });
  }

  private UserAccount newUser(String username, String password) {
    if (username.length() < 3 || username.length() > 32) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "用户名需要 3 到 32 个字符");
    }
    if (password.length() < 6 || password.length() > 72) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "密码需要 6 到 72 个字符");
    }
    UserAccount user = new UserAccount();
    user.setUsername(username);
    user.setPasswordHash(encoder.encode(password));
    user.setRole(Vocab.Role.USER);
    return user;
  }

  public Models.PageResult<Models.UserView> users(int page, int size, String username, String role,
                                                  String memberUntil, String sort, String dir) {
    java.util.List<org.springframework.data.jpa.domain.Specification<com.xtv.domain.UserAccount>> parts = new java.util.ArrayList<>();
    AdminQuery.add(parts, AdminQuery.contains("username", username));
    AdminQuery.add(parts, AdminQuery.eqEnum("role", com.xtv.domain.Vocab.Role.class, role));
    AdminQuery.add(parts, AdminQuery.instantContains("memberUntil", memberUntil));
    var result = users.findAll(AdminQuery.and(parts),
        AdminQuery.page(page, size, sort, dir, "id", java.util.Set.of("id", "username", "role", "memberUntil")));
    return new Models.PageResult<>(result.getContent().stream()
        .map(user -> new Models.UserView(user.getId(), user.getUsername(), user.getRole().name(), user.getMemberUntil()))
        .toList(), result.getNumber() + 1, result.getSize(), result.getTotalElements());
  }

  public void resetPassword(Long id, Requests.PasswordResetRequest req) {
    UserAccount user = users.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在"));
    user.setPasswordHash(encoder.encode(req.password()));
    users.save(user);
  }

  @Transactional
  public Models.UserView createUser(Requests.AdminUserCreate req) {
    if (users.existsByUsername(req.username().trim())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "用户名已存在");
    }
    UserAccount user = newUser(req.username().trim(), req.password());
    user.setRole(parseRole(req.role()));
    return view(users.save(user));
  }

  @Transactional
  public Models.UserView updateUser(Long id, Requests.AdminUserUpdate req) {
    UserAccount user = users.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在"));
    if (req.role() != null && !req.role().isBlank()) {
      Vocab.Role next = parseRole(req.role());
      if (user.getRole() == Vocab.Role.ADMIN && next != Vocab.Role.ADMIN) {
        guardLastAdmin(user);
      }
      user.setRole(next);
    }
    if (req.password() != null && !req.password().isBlank()) {
      if (req.password().length() < 6 || req.password().length() > 72) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "密码需要 6 到 72 个字符");
      }
      user.setPasswordHash(encoder.encode(req.password()));
    }
    return view(users.save(user));
  }

  @Transactional
  public void deleteUser(Long id) {
    UserAccount actor = CurrentUser.require();
    if (actor.getId().equals(id)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "不能删除当前登录的账号");
    }
    UserAccount user = users.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在"));
    if (user.getRole() == Vocab.Role.ADMIN) {
      guardLastAdmin(user);
    }
    favorites.deleteByUserId(id);
    entitlements.deleteByUserId(id);
    users.delete(user);
  }

  private void guardLastAdmin(UserAccount user) {
    if (user.getRole() == Vocab.Role.ADMIN && users.countByRole(Vocab.Role.ADMIN) <= 1) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "至少保留一个管理员");
    }
  }

  private Vocab.Role parseRole(String role) {
    return "ADMIN".equals(role) ? Vocab.Role.ADMIN : Vocab.Role.USER;
  }

  private Models.UserView view(UserAccount user) {
    return new Models.UserView(user.getId(), user.getUsername(), user.getRole().name(), user.getMemberUntil());
  }

  public void changePassword(UserAccount user, Requests.PasswordChangeRequest req) {
    if (!encoder.matches(req.currentPassword(), user.getPasswordHash())) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "当前密码错误");
    }
    if (encoder.matches(req.newPassword(), user.getPasswordHash())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "新密码不能与当前密码相同");
    }
    user.setPasswordHash(encoder.encode(req.newPassword()));
    users.save(user);
  }

  public Models.MeResponse me(UserAccount user) {
    UserAccount fresh = users.findById(user.getId()).orElseThrow();
    List<Long> owned = entitlements.findByUserId(fresh.getId()).stream().map(item -> item.getVideoId()).toList();
    return new Models.MeResponse(fresh.getId(), fresh.getUsername(), fresh.getRole().name(), fresh.getMemberUntil(), owned);
  }

  public UserAccount parse(String token) {
    Claims claims = Jwts.parser().verifyWith(key()).build().parseSignedClaims(token).getPayload();
    Long id = claims.get("uid", Number.class).longValue();
    return users.findById(id).orElseThrow();
  }

  private Models.AuthResponse token(UserAccount user) {
    Instant now = Instant.now();
    String token = Jwts.builder()
        .subject(user.getUsername())
        .claim("uid", user.getId())
        .claim("role", user.getRole().name())
        .issuedAt(Date.from(now))
        .expiration(Date.from(now.plusSeconds(7 * 24 * 3600)))
        .signWith(key())
        .compact();
    return new Models.AuthResponse(token, user.getId(), user.getUsername(), user.getRole().name(), user.getMemberUntil());
  }

  private SecretKey key() {
    return Keys.hmacShaKeyFor(properties.getJwtSecret().getBytes(StandardCharsets.UTF_8));
  }
}
