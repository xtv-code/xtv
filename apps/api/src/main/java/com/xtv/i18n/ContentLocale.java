package com.xtv.i18n;

import java.util.Locale;

public final class ContentLocale {
  private static final ThreadLocal<String> LOCALE = new ThreadLocal<>();
  private static final ThreadLocal<Boolean> LOCALIZE = ThreadLocal.withInitial(() -> Boolean.TRUE);

  private ContentLocale() {}

  public static void bind(String header, boolean localize) {
    LOCALIZE.set(localize);
    LOCALE.set(resolve(header));
  }

  public static void clear() {
    LOCALE.remove();
    LOCALIZE.remove();
  }

  public static boolean localize() {
    return !Boolean.FALSE.equals(LOCALIZE.get());
  }

  public static String get() {
    String value = LOCALE.get();
    return value == null ? "zh" : value;
  }

  public static String resolve(String header) {
    if (header == null || header.isBlank()) {
      return "zh";
    }
    String token = header.split(",")[0].trim().toLowerCase(Locale.ROOT);
    int dash = token.indexOf('-');
    if (dash > 0) {
      token = token.substring(0, dash);
    }
    return switch (token) {
      case "en", "ja", "ko", "zh" -> token;
      default -> "zh";
    };
  }
}
