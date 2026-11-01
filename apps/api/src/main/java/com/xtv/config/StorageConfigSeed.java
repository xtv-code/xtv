package com.xtv.config;

import com.xtv.domain.PlatformConfig;
import com.xtv.repo.PlatformConfigRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(21)
public class StorageConfigSeed implements ApplicationRunner {
  private final PlatformConfigRepository configs;
  private final XtvProperties properties;

  public StorageConfigSeed(PlatformConfigRepository configs, XtvProperties properties) {
    this.configs = configs;
    this.properties = properties;
  }

  @Override
  public void run(ApplicationArguments args) {
    PlatformConfig cfg = configs.findById(1L).orElse(null);
    if (cfg == null || cfg.isS3Seeded()) {
      return;
    }
    XtvProperties.S3 s3 = properties.getS3();
    if (blank(cfg.getS3Endpoint()).isBlank()) {
      cfg.setS3Endpoint(blank(s3.getEndpoint()));
    }
    if (blank(cfg.getS3Region()).isBlank()) {
      cfg.setS3Region(blank(s3.getRegion()).isBlank() ? "us-east-1" : blank(s3.getRegion()));
    }
    if (blank(cfg.getS3Bucket()).isBlank()) {
      cfg.setS3Bucket(blank(s3.getBucket()));
    }
    if (blank(cfg.getS3AccessKey()).isBlank()) {
      cfg.setS3AccessKey(blank(s3.getAccessKey()));
    }
    if (blank(cfg.getS3SecretKey()).isBlank()) {
      cfg.setS3SecretKey(blank(s3.getSecretKey()));
    }
    cfg.setS3Seeded(true);
    configs.save(cfg);
  }

  private static String blank(String value) {
    return value == null ? "" : value.trim();
  }
}
