package com.xtv.domain;

public final class Vocab {
  private Vocab() {}

  public enum Role { ADMIN, USER }

  public enum VideoStatus { DRAFT, UPLOADED, TRANSCODING, READY, FAILED, PUBLISHED }

  public enum AccessType { FREE, MEMBER, PPV }

  public enum JobState { PENDING, RUNNING, SUCCEEDED, FAILED }

  public enum PersonKind { PERFORMER, CREATOR }

  public enum SeriesStatus { ONGOING, COMPLETED }

  public enum StorageMode { LOCAL, OBJECT }

  public enum DeliveryMode { ORIGIN, CDN }

  public enum AdSlot { TOP, PLAYER_BOTTOM, POPUP }

  public enum OrderType { MEMBERSHIP, PPV }

  public enum OrderState { PENDING, PAID, EXPIRED }

  public enum PaymentMethod {
    USDT_TRC20, USDT_BEP20, USDT_ERC20, USDT_POLYGON, USDT_ARBITRUM, USDT_AVALANCHE, USDT_SOLANA, USDT_TON,
    BINANCE_PAY, GATE_PAY, BYBIT_PAY
  }

  public enum TronNetwork { MAINNET, NILE }

  public enum BscNetwork { MAINNET, TESTNET }
}
