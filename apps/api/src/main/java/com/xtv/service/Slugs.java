package com.xtv.service;

import java.util.Locale;
import java.util.function.Predicate;

public final class Slugs {
  private Slugs() {}

  public static String of(String name) {
    if (name == null) {
      return "";
    }
    String normalized = name.trim().toLowerCase(Locale.ROOT)
        .replaceAll("[^\\p{L}\\p{N}]+", "-")
        .replaceAll("^-|-$", "");
    if (normalized.length() > 80) {
      normalized = normalized.substring(0, 80).replaceAll("-$", "");
    }
    return normalized;
  }

  public static String unique(String name, Predicate<String> taken) {
    String base = of(name);
    if (base.isBlank()) {
      base = "item";
    }
    String slug = base;
    int n = 2;
    while (taken.test(slug)) {
      slug = base + "-" + n++;
    }
    return slug;
  }
}
