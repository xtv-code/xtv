package com.xtv.repo;

import com.xtv.domain.Category;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CategoryRepository extends JpaRepository<Category, Long>, JpaSpecificationExecutor<Category> {
  Optional<Category> findBySlug(String slug);
  boolean existsBySlug(String slug);
  List<Category> findByShowInNavTrueOrderBySortOrderAsc();
  List<Category> findByShowOnHomeTrueOrderBySortOrderAsc();
}
