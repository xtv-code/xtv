package com.xtv.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "chain_receiver")
public class ChainReceiver {
  @Id
  @Column(length = 32)
  private String code;

  @Column(nullable = false, length = 128)
  private String address = "";

  @Column(nullable = false, length = 16)
  private String network = "MAINNET";
}
