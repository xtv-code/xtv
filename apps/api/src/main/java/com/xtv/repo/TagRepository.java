package com.xtv.repo;

import com.xtv.domain.Tag;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TagRepository extends JpaRepository<Tag, Long>, JpaSpecificationExecutor<Tag> {
  Optional<Tag> findBySlug(String slug);
  boolean existsBySlug(String slug);
  List<Tag> findByShowInNavTrueOrderBySortOrderAsc();
  List<Tag> findByShowOnHomeTrueOrderBySortOrderAsc();
}
