package com.xtv.config;

import com.xtv.domain.PlatformConfig;
import com.xtv.repo.PlatformConfigRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(20)
public class PaymentConfigSeed implements ApplicationRunner {
  private final PlatformConfigRepository configs;
  private final XtvProperties properties;

  public PaymentConfigSeed(PlatformConfigRepository configs, XtvProperties properties) {
    this.configs = configs;
    this.properties = properties;
  }

  @Override
  public void run(ApplicationArguments args) {
    PlatformConfig cfg = configs.findById(1L).orElse(null);
    if (cfg == null || cfg.isPaymentSeeded()) {
      return;
    }
    cfg.setPaymentSimulate(properties.isPaymentSimulate());
    if (cfg.getUsdtAddress() == null || cfg.getUsdtAddress().isBlank()) {
      cfg.setUsdtAddress(blank(properties.getUsdt().getReceiveAddress()));
    }
    if (cfg.getTronApiKey() == null || cfg.getTronApiKey().isBlank()) {
      cfg.setTronApiKey(blank(properties.getUsdt().getApiKey()));
    }
    cfg.setPaymentSeeded(true);
    configs.save(cfg);
  }

  private static String blank(String value) {
    return value == null ? "" : value.trim();
  }
}
