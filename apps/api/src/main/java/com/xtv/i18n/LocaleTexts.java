package com.xtv.i18n;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public final class LocaleTexts {
  public static final List<String> LOCALES = List.of("zh", "en", "ja", "ko");
  private static final ObjectMapper JSON = new ObjectMapper();
  private static final TypeReference<Map<String, String>> MAP = new TypeReference<>() {};

  private LocaleTexts() {}

  public record Stored(String primary, String json) {}

  public static Stored apply(String single, Map<String, String> names, int max) {
    Map<String, String> clean = new LinkedHashMap<>();
    if (names != null) {
      for (String locale : LOCALES) {
        String raw = names.get(locale);
        if (raw == null) {
          continue;
        }
        String text = raw.trim();
        if (text.isEmpty()) {
          continue;
        }
        if (text.length() > max) {
          throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "每种语言最多 " + max + " 个字符");
        }
        clean.put(locale, text);
      }
    }
    if (clean.isEmpty()) {
      String text = single == null ? "" : single.trim();
      if (text.isEmpty()) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "至少填写一种语言");
      }
      if (text.length() > max) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "每种语言最多 " + max + " 个字符");
      }
      clean.put("zh", text);
    }
    String primary = clean.containsKey("zh") ? clean.get("zh") : clean.values().iterator().next();
    try {
      return new Stored(primary, JSON.writeValueAsString(clean));
    } catch (Exception ex) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "名称无法保存");
    }
  }

  public static String show(String json, String primary) {
    String fallback = primary == null ? "" : primary;
    if (!ContentLocale.localize()) {
      return fallback;
    }
    String value = parse(json).get(ContentLocale.get());
    if (value != null && !value.isBlank()) {
      return value;
    }
    return fallback;
  }

  public static Map<String, String> form(String json, String primary) {
    Map<String, String> parsed = parse(json);
    Map<String, String> out = new LinkedHashMap<>();
    boolean legacy = parsed.isEmpty();
    for (String locale : LOCALES) {
      String value = parsed.get(locale);
      if ((value == null || value.isBlank()) && legacy && "zh".equals(locale) && primary != null) {
        value = primary;
      }
      out.put(locale, value == null ? "" : value);
    }
    return out;
  }

  public static boolean matches(String json, String name, String primary) {
    if (name == null || name.isBlank()) {
      return false;
    }
    String wanted = name.trim();
    if (primary != null && primary.equalsIgnoreCase(wanted)) {
      return true;
    }
    for (String value : parse(json).values()) {
      if (value.equalsIgnoreCase(wanted)) {
        return true;
      }
    }
    return false;
  }

  private static Map<String, String> parse(String json) {
    if (json == null || json.isBlank()) {
      return Map.of();
    }
    try {
      Map<String, String> raw = JSON.readValue(json, MAP);
      return raw == null ? Map.of() : raw;
    } catch (Exception ex) {
      return Map.of();
    }
  }
}
