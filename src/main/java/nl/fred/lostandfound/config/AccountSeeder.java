package nl.fred.lostandfound.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.fred.lostandfound.data.repository.AccountRepository;
import nl.fred.lostandfound.data.repository.UserRepository;
import nl.fred.lostandfound.domain.entity.Account;
import nl.fred.lostandfound.domain.entity.Account.Role;
import nl.fred.lostandfound.domain.entity.User;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AccountSeeder implements CommandLineRunner {

  private static final String SEED_PASSWORD = "password123";

  private final UserRepository userRepository;
  private final AccountRepository accountRepository;
  private final PasswordEncoder passwordEncoder;

  @Override
  public void run(final String... args) {
    if (accountRepository.count() > 0) {
      return;
    }

    final String hash = passwordEncoder.encode(SEED_PASSWORD);

    seed("alice", "Alice Johnson", hash, Role.USER);
    seed("brian", "Brian Smith", hash, Role.USER);
    seed("carla", "Carla Mendes", hash, Role.USER);
    seed("admin", "Admin", hash, Role.ADMIN);

    log.info("Seeded {} demo account(s).", 4);
  }

  private void seed(final String username, final String name, final String passwordHash, final Role role) {
    final User user = userRepository.save(User.builder().name(name).build());

    accountRepository.save(Account.builder()
        .username(username)
        .passwordHash(passwordHash)
        .role(role)
        .user(user)
        .build());
  }

}
