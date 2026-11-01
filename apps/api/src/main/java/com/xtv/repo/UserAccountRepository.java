package com.xtv.repo;

import com.xtv.domain.UserAccount;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long>, JpaSpecificationExecutor<UserAccount> {
  Optional<UserAccount> findByUsername(String username);
  boolean existsByUsername(String username);
  boolean existsByRole(com.xtv.domain.Vocab.Role role);
  long countByRole(com.xtv.domain.Vocab.Role role);
}
