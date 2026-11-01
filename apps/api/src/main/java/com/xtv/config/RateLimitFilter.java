package com.xtv.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class RateLimitFilter extends OncePerRequestFilter {
  private final Map<String, Deque<Long>> buckets = new ConcurrentHashMap<>();

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    Limit limit = limitFor(request);
    if (limit != null && !allow(limit.key(), limit.max(), limit.windowSeconds())) {
      response.setStatus(429);
      response.setContentType(MediaType.APPLICATION_JSON_VALUE);
      response.getWriter().write("{\"message\":\"请求太频繁，请稍后再试\"}");
      return;
    }
    filterChain.doFilter(request, response);
  }

  private Limit limitFor(HttpServletRequest request) {
    String path = request.getRequestURI();
    String method = request.getMethod();
    String ip = clientIp(request);
    if ("POST".equals(method) && path.equals("/api/v1/auth/login")) {
      return new Limit("login:" + ip, 20, 60);
    }
    if ("POST".equals(method) && path.equals("/api/v1/auth/register")) {
      return new Limit("register:" + ip, 10, 60);
    }
    if ("POST".equals(method) && path.equals("/api/v1/orders")) {
      return new Limit("orders:" + ip, 30, 60);
    }
    // Order pages poll the chain about every 10s; keep headroom for refresh + manual check.
    if ("POST".equals(method) && path.matches("/api/v1/orders/\\d+/check")) {
      return new Limit("order-check:" + ip, 60, 60);
    }
    if ("POST".equals(method) && path.equals("/api/v1/reports")) {
      return new Limit("reports:" + ip, 8, 60);
    }
    if ("POST".equals(method) && path.matches("/api/v1/videos/\\d+/views")) {
      return new Limit("views:" + ip, 60, 60);
    }
    return null;
  }

  private boolean allow(String key, int max, int windowSeconds) {
    long now = Instant.now().getEpochSecond();
    long cutoff = now - windowSeconds;
    Deque<Long> q = buckets.computeIfAbsent(key, ignored -> new ArrayDeque<>());
    synchronized (q) {
      while (!q.isEmpty() && q.peekFirst() < cutoff) {
        q.pollFirst();
      }
      if (q.size() >= max) {
        return false;
      }
      q.addLast(now);
      return true;
    }
  }

  private static String clientIp(HttpServletRequest request) {
    String forwarded = request.getHeader("X-Forwarded-For");
    if (forwarded != null && !forwarded.isBlank()) {
      return forwarded.split(",")[0].trim();
    }
    return request.getRemoteAddr() == null ? "unknown" : request.getRemoteAddr();
  }

  private record Limit(String key, int max, int windowSeconds) {}
}