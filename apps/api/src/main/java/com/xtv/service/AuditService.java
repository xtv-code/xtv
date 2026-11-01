package com.xtv.service;

import com.xtv.domain.AuditLog;
import com.xtv.domain.UserAccount;
import com.xtv.repo.AuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
public class AuditService {
  private final AuditLogRepository logs;

  public AuditService(AuditLogRepository logs) {
    this.logs = logs;
  }

  @Transactional
  public void record(String action, String targetType, String targetId, String detail) {
    UserAccount actor = CurrentUser.optional();
    AuditLog row = new AuditLog();
    if (actor != null) {
      row.setActorId(actor.getId());
      row.setActorName(actor.getUsername());
    }
    row.setAction(action);
    row.setTargetType(targetType == null ? "" : targetType);
    row.setTargetId(targetId == null ? "" : targetId);
    row.setDetail(detail == null ? "" : detail.length() > 1000 ? detail.substring(0, 1000) : detail);
    row.setIp(clientIp());
    logs.save(row);
  }

  private static String clientIp() {
    try {
      var attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
      if (attrs == null) {
        return "";
      }
      var request = attrs.getRequest();
      String forwarded = request.getHeader("X-Forwarded-For");
      if (forwarded != null && !forwarded.isBlank()) {
        return forwarded.split(",")[0].trim();
      }
      return request.getRemoteAddr() == null ? "" : request.getRemoteAddr();
    } catch (Exception ignored) {
      return "";
    }
  }
}