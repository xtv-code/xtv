package com.xtv.service;

import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

public final class AdminQuery {
  private AdminQuery() {}

  public static Pageable page(int page, int size, String sort, String dir, String fallback, Set<String> allowed) {
    String field = sort == null || sort.isBlank() ? fallback : sort.trim();
    if (!allowed.contains(field)) {
      field = fallback;
    }
    Sort.Direction direction = "asc".equalsIgnoreCase(dir) ? Sort.Direction.ASC : Sort.Direction.DESC;
    return PageRequest.of(Math.max(page, 1) - 1, Math.min(Math.max(size, 1), 200), Sort.by(direction, field));
  }

  public static Sort sortOf(String sort, String dir, String fallback, Set<String> allowed) {
    return page(1, 1, sort, dir, fallback, allowed).getSort();
  }

  public static boolean blank(String value) {
    return value == null || value.isBlank();
  }

  public static String text(String value) {
    return value == null ? "" : value.trim();
  }

  public static <T> Specification<T> and(List<Specification<T>> parts) {
    Specification<T> all = null;
    for (Specification<T> part : parts) {
      if (part == null) {
        continue;
      }
      all = all == null ? part : all.and(part);
    }
    return all == null ? (root, query, cb) -> cb.conjunction() : all;
  }

  public static <T> void add(List<Specification<T>> parts, Specification<T> part) {
    if (part != null) {
      parts.add(part);
    }
  }

  public static <T> Specification<T> contains(String field, String value) {
    if (blank(value)) {
      return null;
    }
    String needle = "%" + text(value).toLowerCase(Locale.ROOT) + "%";
    return (root, query, cb) -> cb.like(cb.lower(root.get(field).as(String.class)), needle);
  }

  public static <T> Specification<T> containsAny(String value, String... fields) {
    if (blank(value) || fields.length == 0) {
      return null;
    }
    String needle = "%" + text(value).toLowerCase(Locale.ROOT) + "%";
    return (root, query, cb) -> {
      List<Predicate> parts = new ArrayList<>();
      for (String field : fields) {
        parts.add(cb.like(cb.lower(root.get(field).as(String.class)), needle));
      }
      return cb.or(parts.toArray(Predicate[]::new));
    };
  }

  public static <T> Specification<T> eqString(String field, String value) {
    if (blank(value)) {
      return null;
    }
    return (root, query, cb) -> cb.equal(root.get(field), text(value));
  }

  public static <T, E extends Enum<E>> Specification<T> eqEnum(String field, Class<E> type, String value) {
    if (blank(value)) {
      return null;
    }
    try {
      E parsed = Enum.valueOf(type, text(value).toUpperCase(Locale.ROOT));
      return (root, query, cb) -> cb.equal(root.get(field), parsed);
    } catch (IllegalArgumentException ex) {
      return (root, query, cb) -> cb.disjunction();
    }
  }

  public static <T> Specification<T> eqLong(String field, String value) {
    if (blank(value)) {
      return null;
    }
    try {
      long id = Long.parseLong(text(value).replace("#", ""));
      return (root, query, cb) -> cb.equal(root.get(field), id);
    } catch (NumberFormatException ex) {
      return (root, query, cb) -> cb.disjunction();
    }
  }

  public static <T> Specification<T> eqBool(String field, String value) {
    if (blank(value)) {
      return null;
    }
    String raw = text(value).toLowerCase(Locale.ROOT);
    Boolean flag = switch (raw) {
      case "1", "true", "yes", "y", "上架", "启用", "导航", "首页" -> true;
      case "0", "false", "no", "n", "下架", "停用" -> false;
      default -> null;
    };
    if (flag == null) {
      return (root, query, cb) -> cb.disjunction();
    }
    boolean wanted = flag;
    return (root, query, cb) -> cb.equal(root.get(field), wanted);
  }

  public static <T> Specification<T> eqBigDecimal(String field, String value) {
    if (blank(value)) {
      return null;
    }
    try {
      BigDecimal amount = new BigDecimal(text(value));
      return (root, query, cb) -> cb.equal(root.get(field), amount);
    } catch (NumberFormatException ex) {
      return (root, query, cb) -> cb.disjunction();
    }
  }

  public static <T> Specification<T> eqInt(String field, String value) {
    if (blank(value)) {
      return null;
    }
    try {
      int number = Integer.parseInt(text(value));
      return (root, query, cb) -> cb.equal(root.get(field), number);
    } catch (NumberFormatException ex) {
      return (root, query, cb) -> cb.disjunction();
    }
  }

  public static <T> Specification<T> instantContains(String field, String value) {
    if (blank(value)) {
      return null;
    }
    String needle = "%" + text(value).replace('T', ' ') + "%";
    return (root, query, cb) -> cb.like(root.get(field).as(String.class), needle);
  }
}
