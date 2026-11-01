package com.xtv.config;

import com.xtv.domain.UserAccount;
import com.xtv.domain.Vocab;
import com.xtv.repo.UserAccountRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminSeed implements ApplicationRunner {
  private final UserAccountRepository users;
  private final PasswordEncoder encoder;
  private final XtvProperties properties;

  public AdminSeed(UserAccountRepository users, PasswordEncoder encoder, XtvProperties properties) {
    this.users = users;
    this.encoder = encoder;
    this.properties = properties;
  }

  @Override
  public void run(ApplicationArguments args) {
    if (users.existsByRole(Vocab.Role.ADMIN)) {
      return;
    }
    UserAccount admin = new UserAccount();
    admin.setUsername(properties.getAdmin().getUsername());
    admin.setPasswordHash(encoder.encode(properties.getAdmin().getPassword()));
    admin.setRole(Vocab.Role.ADMIN);
    users.save(admin);
  }
}
