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
@Table(name = "legal_document")
public class LegalDocument {
  @Id
  @Column(length = 16)
  private String code;

  @Column(nullable = false, columnDefinition = "LONGTEXT")
  private String body = "";
}
