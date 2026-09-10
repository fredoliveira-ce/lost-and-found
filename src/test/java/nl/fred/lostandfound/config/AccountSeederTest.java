package nl.fred.lostandfound.config;

import static org.assertj.core.api.Assertions.assertThat;

import nl.fred.lostandfound.data.repository.AccountRepository;
import nl.fred.lostandfound.domain.entity.Account;
import nl.fred.lostandfound.domain.entity.Account.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.security.crypto.password.PasswordEncoder;

@DisplayName("Runs all tests for AccountSeeder")
class AccountSeederTest {

  private AccountSeeder seeder;
  private AccountRepository accountRepository;
  private PasswordEncoder passwordEncoder;

  @BeforeEach
  void beforeEach() {
    accountRepository = Mockito.mock(AccountRepository.class);
    passwordEncoder = Mockito.mock(PasswordEncoder.class);
    seeder = new AccountSeeder(accountRepository, passwordEncoder);
  }

  @Test
  @DisplayName("should do nothing when accounts already exist")
  void doesNothingWhenAccountsAlreadyExist() {
    Mockito.when(accountRepository.count()).thenReturn(1L);

    seeder.run();

    Mockito.verifyNoInteractions(passwordEncoder);
    Mockito.verify(accountRepository, Mockito.never()).save(Mockito.any());
  }

  @Test
  @DisplayName("should seed all four demo accounts when the repository is empty")
  void seedsAllAccountsWhenRepositoryIsEmpty() {
    Mockito.when(accountRepository.count()).thenReturn(0L);
    Mockito.when(passwordEncoder.encode(Mockito.any())).thenReturn("hashed");

    seeder.run();

    ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
    Mockito.verify(accountRepository, Mockito.times(4)).save(captor.capture());

    assertThat(captor.getAllValues())
        .extracting(Account::getId, Account::getUsername, Account::getPasswordHash, Account::getRole)
        .containsExactly(
            org.assertj.core.groups.Tuple.tuple(1001L, "alice", "hashed", Role.USER),
            org.assertj.core.groups.Tuple.tuple(1002L, "brian", "hashed", Role.USER),
            org.assertj.core.groups.Tuple.tuple(1003L, "carla", "hashed", Role.USER),
            org.assertj.core.groups.Tuple.tuple(9001L, "admin", "hashed", Role.ADMIN));
  }

}
