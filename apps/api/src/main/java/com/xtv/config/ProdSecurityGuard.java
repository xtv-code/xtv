package com.xtv.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class ProdSecurityGuard implements ApplicationRunner {
  private static final Logger log = LoggerFactory.getLogger(ProdSecurityGuard.class);
  private static final String DEFAULT_JWT = "xtv-dev-jwt-secret-change-me-32b";
  private static final String DEFAULT_PASSWORD = "admin123";

  private final Environment env;
  private final XtvProperties properties;

  public ProdSecurityGuard(Environment env, XtvProperties properties) {
    this.env = env;
    this.properties = properties;
  }

  @Override
  public void run(ApplicationArguments args) {
    boolean prod = false;
    for (String profile : env.getActiveProfiles()) {
      if ("prod".equalsIgnoreCase(profile)) {
        prod = true;
        break;
      }
    }
    if (!prod) {
      return;
    }
    if (DEFAULT_JWT.equals(properties.getJwtSecret()) || properties.getJwtSecret() == null
        || properties.getJwtSecret().length() < 32) {
      throw new IllegalStateException("生产环境必须设置至少 32 位的 XTV_JWT_SECRET，且不能使用默认值");
    }
    if (DEFAULT_PASSWORD.equals(properties.getAdmin().getPassword())) {
      throw new IllegalStateException("生产环境必须设置 XTV_ADMIN_PASSWORD，且不能使用默认 admin123");
    }
    if (properties.isPaymentSimulate()) {
      throw new IllegalStateException("生产环境禁止开启模拟支付（xtv.payment-simulate）");
    }
    log.info("生产安全检查通过");
  }
}