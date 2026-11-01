package com.xtv.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "xtv")
public class XtvProperties {
  private String storageRoot = "data/media";
  private boolean jobsEnabled = true;
  private boolean paymentSimulate = false;
  /** Local profile turns this on to fill an empty-looking catalog once. */
  private boolean catalogSeed = false;
  /** When true, first login with a new username creates the account. Disable in production. */
  private boolean autoRegister = true;
  private String jwtSecret = "xtv-dev-jwt-secret-change-me-32b";
  /** Comma-separated Spring origin patterns. Prod should set XTV_CORS_ORIGINS to the public web origin. */
  private String corsOrigins = "http://localhost:*,http://127.0.0.1:*,http://[::1]:*,http://10.*:*,http://192.168.*:*,http://172.*:*";
  private Admin admin = new Admin();
  private Usdt usdt = new Usdt();
  private S3 s3 = new S3();

  @Getter
  @Setter
  public static class Admin {
    private String username = "admin";
    private String password = "admin123";
  }

  @Getter
  @Setter
  public static class Usdt {
    private String receiveAddress = "";
    private String apiKey = "";
    private String trongridBase = "https://api.trongrid.io";
  }

  @Getter
  @Setter
  public static class S3 {
    private String endpoint = "";
    private String region = "us-east-1";
    private String bucket = "";
    private String accessKey = "";
    private String secretKey = "";
  }
}
