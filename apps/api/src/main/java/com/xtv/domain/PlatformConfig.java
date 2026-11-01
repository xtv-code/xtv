package com.xtv.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "platform_config")
public class PlatformConfig {
  @Id
  private Long id;

  @Enumerated(EnumType.STRING)
  @Column(name = "storage_mode", nullable = false, length = 16)
  private Vocab.StorageMode storageMode;

  @Enumerated(EnumType.STRING)
  @Column(name = "delivery_mode", nullable = false, length = 16)
  private Vocab.DeliveryMode deliveryMode;

  @Column(name = "origin_base", nullable = false, length = 300)
  private String originBase;

  @Column(name = "cdn_base", length = 300)
  private String cdnBase;

  @Column(name = "usdt_address", nullable = false, length = 64)
  private String usdtAddress = "";

  @Column(name = "payment_simulate", nullable = false)
  private boolean paymentSimulate;

  @Column(name = "tron_api_key", nullable = false, length = 200)
  private String tronApiKey = "";

  @Enumerated(EnumType.STRING)
  @Column(name = "tron_network", nullable = false, length = 16)
  private Vocab.TronNetwork tronNetwork = Vocab.TronNetwork.MAINNET;

  @Column(name = "bsc_address", nullable = false, length = 64)
  private String bscAddress = "";

  @Enumerated(EnumType.STRING)
  @Column(name = "bsc_network", nullable = false, length = 16)
  private Vocab.BscNetwork bscNetwork = Vocab.BscNetwork.MAINNET;

  @Column(name = "payment_seeded", nullable = false)
  private boolean paymentSeeded;

  @Column(name = "site_name", nullable = false, length = 40)
  private String siteName = "XTV";

  @Column(name = "site_tagline", nullable = false, length = 200)
  private String siteTagline = "点播平台。本地转码、会员订阅与单集购买，面向 Web 与 H5。";

  @Column(name = "catalog_seeded", nullable = false)
  private boolean catalogSeeded;

  @Column(name = "s3_endpoint", nullable = false, length = 300)
  private String s3Endpoint = "";

  @Column(name = "s3_region", nullable = false, length = 64)
  private String s3Region = "us-east-1";

  @Column(name = "s3_bucket", nullable = false, length = 128)
  private String s3Bucket = "";

  @Column(name = "s3_access_key", nullable = false, length = 200)
  private String s3AccessKey = "";

  @Column(name = "s3_secret_key", nullable = false, length = 200)
  private String s3SecretKey = "";

  @Column(name = "s3_seeded", nullable = false)
  private boolean s3Seeded;
}
