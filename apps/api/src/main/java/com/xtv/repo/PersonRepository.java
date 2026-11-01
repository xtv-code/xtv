package com.xtv.repo;

import com.xtv.domain.Person;
import com.xtv.domain.Vocab;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface PersonRepository extends JpaRepository<Person, Long>, JpaSpecificationExecutor<Person> {
  Optional<Person> findBySlug(String slug);
  boolean existsBySlug(String slug);
  Page<Person> findByKind(Vocab.PersonKind kind, Pageable pageable);
}
