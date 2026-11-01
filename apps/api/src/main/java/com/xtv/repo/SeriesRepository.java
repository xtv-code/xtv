package com.xtv.repo;

import com.xtv.domain.Series;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface SeriesRepository extends JpaRepository<Series, Long>, JpaSpecificationExecutor<Series> {
  Optional<Series> findBySlug(String slug);
  boolean existsBySlug(String slug);
}
