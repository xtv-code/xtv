package com.xtv.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class Category {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 80)
  private String name;

  @Column(nullable = false, length = 4000)
  private String i18n = "";

  @Column(nullable = false, unique = true, length = 120)
  private String slug;

  @Column(name = "sort_order", nullable = false)
  private int sortOrder;

  @Column(name = "show_in_nav", nullable = false)
  private boolean showInNav;

  @Column(name = "show_on_home", nullable = false)
  private boolean showOnHome;
}
