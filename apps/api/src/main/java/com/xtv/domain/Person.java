package com.xtv.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class Person {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 120)
  private String name;

  @Column(nullable = false, length = 4000)
  private String i18n = "";

  @Column(name = "avatar_key", length = 300)
  private String avatarKey;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  private Vocab.PersonKind kind;

  @Column(nullable = false, unique = true, length = 120)
  private String slug;
}
