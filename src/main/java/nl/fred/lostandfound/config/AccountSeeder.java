package nl.fred.lostandfound.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.fred.lostandfound.data.repository.AccountRepository;
import nl.fred.lostandfound.domain.entity.Account;
import nl.fred.lostandfound.domain.entity.Account.Role;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AccountSeeder implements CommandLineRunner {

    private static final String SEED_PASSWORD = "password123";

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(final String... args) {
        if (accountRepository.count() > 0) {
            return;
        }

        final String hash = passwordEncoder.encode(SEED_PASSWORD);

        seed(1001L, "alice", hash, Role.USER);
        seed(1002L, "brian", hash, Role.USER);
        seed(1003L, "carla", hash, Role.USER);
        seed(9001L, "admin", hash, Role.ADMIN);

        log.info("Seeded {} demo account(s).", 4);
    }

    private void seed(final Long id, final String username, final String passwordHash, final Role role) {
        accountRepository.save(Account.builder()
                .id(id)
                .username(username)
                .passwordHash(passwordHash)
                .role(role)
                .build());
    }

}
