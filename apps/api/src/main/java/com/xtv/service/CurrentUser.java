package com.xtv.service;

import com.xtv.domain.UserAccount;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

public final class CurrentUser {
  private CurrentUser() {}

  public static UserAccount optional() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !(auth.getPrincipal() instanceof UserAccount user)) {
      return null;
    }
    return user;
  }

  public static UserAccount require() {
    UserAccount user = optional();
    if (user == null) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "未登录");
    }
    return user;
  }
}
