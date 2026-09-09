package nl.fred.lostandfound.domain.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import nl.fred.lostandfound.data.repository.AccountRepository;
import nl.fred.lostandfound.domain.entity.Account;
import nl.fred.lostandfound.domain.exception.InvalidCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

  private static final long TOKEN_LIFETIME_HOURS = 1;

  private final AccountRepository accountRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtEncoder jwtEncoder;

  public String login(final String username, final String password) {
    final Account account = accountRepository.findByUsername(username)
        .orElseThrow(InvalidCredentialsException::new);

    if (!passwordEncoder.matches(password, account.getPasswordHash())) {
      throw new InvalidCredentialsException();
    }

    final Instant now = Instant.now();

    final JwtClaimsSet claims = JwtClaimsSet.builder()
        .issuer("lost-and-found")
        .issuedAt(now)
        .expiresAt(now.plus(TOKEN_LIFETIME_HOURS, ChronoUnit.HOURS))
        .subject(account.getUsername())
        .claim("uid", account.getUser().getId())
        .claim("role", account.getRole().name())
        .build();

    return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
  }

}
