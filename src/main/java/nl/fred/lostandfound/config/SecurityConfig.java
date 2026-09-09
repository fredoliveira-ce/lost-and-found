package nl.fred.lostandfound.config;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import nl.fred.lostandfound.domain.exception.ApiException.ApiExceptionType;
import nl.fred.lostandfound.web.dto.ApiErrorResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import tools.jackson.databind.json.JsonMapper;


@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

  private final JsonMapper jsonMapper;

  @Bean
  public KeyPair rsaKeyPair() throws NoSuchAlgorithmException {
    final KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
    generator.initialize(2048);
    return generator.generateKeyPair();
  }

  @Bean
  public JWKSource<SecurityContext> jwkSource(final KeyPair rsaKeyPair) {
    final RSAKey rsaKey = new RSAKey.Builder((RSAPublicKey) rsaKeyPair.getPublic())
        .privateKey((RSAPrivateKey) rsaKeyPair.getPrivate())
        .keyID(UUID.randomUUID().toString())
        .build();

    return new ImmutableJWKSet<>(new JWKSet(rsaKey));
  }

  @Bean
  public JwtEncoder jwtEncoder(final JWKSource<SecurityContext> jwkSource) {
    return new NimbusJwtEncoder(jwkSource);
  }

  @Bean
  public JwtDecoder jwtDecoder(final KeyPair rsaKeyPair) {
    final NimbusJwtDecoder decoder = NimbusJwtDecoder
        .withPublicKey((RSAPublicKey) rsaKeyPair.getPublic())
        .build();
    decoder.setJwtValidator(JwtValidators.createDefault());
    return decoder;
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public JwtAuthenticationConverter jwtAuthenticationConverter() {
    final JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
    authoritiesConverter.setAuthoritiesClaimName("role");
    authoritiesConverter.setAuthorityPrefix("ROLE_");

    final JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
    return converter;
  }

  @Bean
  public SecurityFilterChain securityFilterChain(
      final HttpSecurity http, final JwtAuthenticationConverter jwtAuthenticationConverter) throws Exception {

    http
        .csrf(csrf -> csrf.disable())
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/api/auth/login").permitAll()
            .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
            .requestMatchers("/api/admin/**").hasRole("ADMIN")
            .anyRequest().authenticated())
        .oauth2ResourceServer(oauth2 -> oauth2
            .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)))
        .exceptionHandling(exceptions -> exceptions
            .authenticationEntryPoint(this::handleAuthenticationFailure)
            .accessDeniedHandler(this::handleAccessDenied));

    return http.build();
  }

  private void handleAuthenticationFailure(
      final HttpServletRequest request, final HttpServletResponse response,
      final AuthenticationException authException)
      throws IOException {
    writeError(response, HttpStatus.UNAUTHORIZED, ApiExceptionType.UNAUTHORIZED,
        "Missing or invalid authentication token.");
  }

  private void handleAccessDenied(
      final HttpServletRequest request, final HttpServletResponse response,
      final AccessDeniedException accessDeniedException)
      throws IOException {
    writeError(response, HttpStatus.FORBIDDEN, ApiExceptionType.FORBIDDEN,
        "You do not have permission to perform this action.");
  }

  private void writeError(
      final HttpServletResponse response, final HttpStatus status, final ApiExceptionType type,
      final String message) throws IOException {
    response.setStatus(status.value());
    response.setContentType(APPLICATION_JSON_VALUE);
    response.getWriter().write(jsonMapper.writeValueAsString(new ApiErrorResponse(type.name(), message)));
  }

}
