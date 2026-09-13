package nl.fred.lostandfound.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import nl.fred.lostandfound.domain.exception.ApiException.ApiExceptionType;
import nl.fred.lostandfound.web.dto.ApiErrorResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
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

import java.io.IOException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.UUID;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;


// PMD ExcessiveImports/CouplingBetweenObjects/TooManyMethods: this class
// wires together JWT encoding/decoding, the RSA key pair (generated or
// loaded from config), the security filter chain, and the error-response
// mapping - one cohesive "security setup" responsibility that genuinely
// needs all these types. Splitting it up would scatter one concern across
// several files rather than simplify anything.
@SuppressWarnings({"PMD.ExcessiveImports", "PMD.CouplingBetweenObjects", "PMD.TooManyMethods"})
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JsonMapper jsonMapper;

    @Value("${app.jwt.private-key:}")
    private String configuredPrivateKey;

    @Value("${app.jwt.public-key:}")
    private String configuredPublicKey;

    @Value("${server.port:8080}")
    private int serverPort;

    @Value("${management.server.port:${server.port:8080}}")
    private int managementPort;

    @Bean
    public KeyPair rsaKeyPair() throws NoSuchAlgorithmException, InvalidKeySpecException {
        final boolean hasPrivateKey = !configuredPrivateKey.isBlank();
        final boolean hasPublicKey = !configuredPublicKey.isBlank();

        if (hasPrivateKey && hasPublicKey) {
            return loadKeyPair(configuredPrivateKey, configuredPublicKey);
        }
        if (hasPrivateKey || hasPublicKey) {
            throw new IllegalStateException(
                    "app.jwt.private-key and app.jwt.public-key must both be set together, or both left unset.");
        }

        final KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    private KeyPair loadKeyPair(final String privateKeyBase64, final String publicKeyBase64)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        final KeyFactory factory = KeyFactory.getInstance("RSA");
        final RSAPrivateKey privateKey = (RSAPrivateKey) factory.generatePrivate(
                new PKCS8EncodedKeySpec(Base64.getDecoder().decode(privateKeyBase64)));
        final RSAPublicKey publicKey = (RSAPublicKey) factory.generatePublic(
                new X509EncodedKeySpec(Base64.getDecoder().decode(publicKeyBase64)));
        return new KeyPair(publicKey, privateKey);
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
        final HttpSecurity http,
        final JwtAuthenticationConverter jwtAuthenticationConverter
    ) {
        requireManagementPortIsSeparate();

        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/login").permitAll()
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(this::handleAuthenticationFailure)
                        .accessDeniedHandler(this::handleAccessDenied));

        return http.build();
    }

    private void requireManagementPortIsSeparate() {
        if (managementPort == serverPort) {
            throw new IllegalStateException(
                    "management.server.port must differ from server.port - actuator is permitAll "
                            + "and relies on running on a separate, network-isolated port.");
        }
    }

    // PMD UnusedFormalParameter: `request`/`authException` are unused in the
    // body, but the method signature is fixed by AuthenticationEntryPoint
    // (used above as a method reference) - it can't be trimmed down.
    @SuppressWarnings("PMD.UnusedFormalParameter")
    private void handleAuthenticationFailure(
        final HttpServletRequest request,
        final HttpServletResponse response,
        final AuthenticationException authException
    ) throws IOException {
        writeError(response, HttpStatus.UNAUTHORIZED, ApiExceptionType.UNAUTHORIZED,
                "Missing or invalid authentication token.");
    }

    // PMD UnusedFormalParameter: same as above - fixed by AccessDeniedHandler.
    @SuppressWarnings("PMD.UnusedFormalParameter")
    private void handleAccessDenied(
        final HttpServletRequest request,
        final HttpServletResponse response,
        final AccessDeniedException accessDeniedException
    ) throws IOException {
        writeError(response, HttpStatus.FORBIDDEN, ApiExceptionType.FORBIDDEN,
                "You do not have permission to perform this action.");
    }

    private void writeError(
        final HttpServletResponse response, final HttpStatus status, final ApiExceptionType type, final String message
    ) throws IOException {
        response.setStatus(status.value());
        response.setContentType(APPLICATION_JSON_VALUE);
        response.getWriter().write(jsonMapper.writeValueAsString(new ApiErrorResponse(type.name(), message)));
    }

}
