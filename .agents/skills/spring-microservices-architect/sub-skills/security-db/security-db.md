# Security DB Sub-Skill — Spring Security 7.0 Factor-Based Database Integration

This sub-skill governs **database-backed authentication and authorization** for Spring Security 7.0 with the integrated Spring Authorization Server. It covers factor-aware schema design, custom `UserDetailsService` for MFA, OAuth2 persistence, `JdbcUserDetailsManager` customization, One-Time Token (OTT) storage, session management, and security auditing.

> `${basePackage}` is a project-configurable placeholder. `<Service>` refers to the target microservice.

---

## Metadata

```yaml
name: security-db
description: >
  Spring Security 7.0 database integration for factor-based authentication.
  Covers MFA-aware schema, custom UserDetailsService, OAuth2 database schema,
  JdbcUserDetailsManager, One-Time Token persistence, session repository, and audit logging.
triggers:
  - security database
  - security db
  - factor authentication
  - MFA
  - multi-factor
  - multifactor
  - WebAuthn
  - passkey
  - passkeys
  - UserDetailsService
  - JdbcUserDetailsManager
  - one-time token
  - OTT
  - magic link
  - passwordless
  - OAuth2 schema
  - authorization server database
  - session repository
  - spring session jdbc
  - security audit
  - password encoder
  - BCrypt
  - Argon2
```

---

## Pre-Checks

Before applying any security-db pattern, execute these checks — **abort if any fail**:

1. **Security layer detected:** Confirm `spring-boot-starter-security` is in `build.gradle`
2. **Persistence layer detected:** Confirm JPA or JDBC dependency exists (`spring-boot-starter-data-jpa` or `spring-boot-starter-jdbc`)
3. **Conventions loaded:** Read [conventions.md](../../references/conventions.md) and [tech-stack.md](../../references/tech-stack.md)
4. **Existing security config scanned:** List all `*SecurityConfig*.java`, `*UserDetails*.java`, and `*AuthorizationServerConfig*.java` files

```bash
# Pre-check: security + persistence present
find . -name "build.gradle" -exec grep -l "spring-boot-starter-security" {} \;
find . -name "build.gradle" -exec grep -l "data-jpa\|starter-jdbc" {} \;

# Pre-check: scan existing security classes
find . -name "*SecurityConfig*.java" | head -20
find . -name "*UserDetails*.java" | head -20
find . -name "*AuthorizationServerConfig*.java" | head -10
```

---

## Component Architecture

```
┌──────────────────────────────────────────────────────────────────┐
│                    Spring Security 7.0 DB Layer                  │
├──────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌──────────────┐  ┌───────────────────┐  ┌──────────────────┐  │
│  │ Factor-Aware  │  │ Custom            │  │ OAuth2 DB        │  │
│  │ Schema        │  │ UserDetailsService│  │ Schema           │  │
│  │               │  │ (MFA metadata)    │  │ (AuthZ Server)   │  │
│  └──────┬───────┘  └────────┬──────────┘  └────────┬─────────┘  │
│         │                   │                      │             │
│  ┌──────▼───────┐  ┌───────▼──────────┐  ┌────────▼─────────┐  │
│  │ JdbcUserDtls  │  │ OneTimeToken     │  │ Session          │  │
│  │ Manager       │  │ Service (OTT)    │  │ Repository       │  │
│  │ (customized)  │  │ (passwordless)   │  │ (distributed)    │  │
│  └──────────────┘  └──────────────────┘  └──────────────────┘  │
│                                                                  │
│  Cross-Cutting: PasswordEncoder (BCrypt/Argon2) │ Audit Logging │
└──────────────────────────────────────────────────────────────────┘
```

---

## Pattern 1 — Factor-Aware Schema Design

Design user tables to support multiple authentication factors beyond username/password.

### 1.1 — Core User Table (JPA)

```java
package ${basePackage}.microservices.core.<service>.persistence;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "users", uniqueConstraints = {
    @UniqueConstraint(columnNames = "username"),
    @UniqueConstraint(columnNames = "email")
})
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String username;

    @Column(nullable = false, length = 255)
    private String email;

    @Column(nullable = false, length = 255)
    private String password;  // BCrypt or Argon2 encoded

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(nullable = false)
    private boolean accountNonLocked = true;

    // ── Factor-Based Authentication Fields ──

    @Column(name = "is_mfa_enabled", nullable = false)
    private boolean mfaEnabled = false;

    @Column(name = "mfa_secret", length = 64)
    private String mfaSecret;  // TOTP shared secret (encrypted at rest)

    @Column(name = "mfa_recovery_codes", length = 1024)
    private String mfaRecoveryCodes;  // Comma-separated hashed recovery codes

    @Lob
    @Column(name = "webauthn_credentials")
    private String webauthnCredentials;  // JSON array of registered Passkey credential IDs

    @Column(name = "preferred_factor", length = 20)
    @Enumerated(EnumType.STRING)
    private AuthenticationFactor preferredFactor = AuthenticationFactor.PASSWORD;

    // ── Audit Fields ──

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @Column(name = "failed_login_attempts", nullable = false)
    private int failedLoginAttempts = 0;

    @Column(name = "lockout_until")
    private Instant lockoutUntil;

    @Version
    private int version;

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    // Getters + Setters omitted — use Lombok @Data or generate
}
```

### 1.2 — Authentication Factor Enum

```java
package ${basePackage}.microservices.core.<service>.persistence;

public enum AuthenticationFactor {
    PASSWORD,
    TOTP,       // Time-based One-Time Password (Google Authenticator, Authy)
    WEBAUTHN,   // Passkeys / FIDO2
    OTT,        // One-Time Token (magic link / passwordless)
    SMS,        // SMS-based OTP (fallback only)
    EMAIL       // Email-based OTP
}
```

### 1.3 — Authority/Role Table

```java
package ${basePackage}.microservices.core.<service>.persistence;

import jakarta.persistence.*;

@Entity
@Table(name = "authorities")
public class AuthorityEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String authority;  // e.g., "ROLE_USER", "SCOPE_product:read"

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;
}
```

### 1.4 — Flyway / SQL Migration

```sql
-- V1__factor_aware_user_schema.sql

CREATE TABLE users (
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    username              VARCHAR(50)   NOT NULL UNIQUE,
    email                 VARCHAR(255)  NOT NULL UNIQUE,
    password              VARCHAR(255)  NOT NULL,
    enabled               BOOLEAN       NOT NULL DEFAULT TRUE,
    account_non_locked    BOOLEAN       NOT NULL DEFAULT TRUE,
    is_mfa_enabled        BOOLEAN       NOT NULL DEFAULT FALSE,
    mfa_secret            VARCHAR(64),
    mfa_recovery_codes    VARCHAR(1024),
    webauthn_credentials  TEXT,
    preferred_factor      VARCHAR(20)   NOT NULL DEFAULT 'PASSWORD',
    created_at            TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP,
    last_login_at         TIMESTAMP,
    failed_login_attempts INT           NOT NULL DEFAULT 0,
    lockout_until         TIMESTAMP,
    version               INT           NOT NULL DEFAULT 0
);

CREATE TABLE authorities (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id   BIGINT      NOT NULL,
    authority VARCHAR(50) NOT NULL,
    CONSTRAINT fk_authorities_users FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uq_user_authority UNIQUE (user_id, authority)
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_authorities_user_id ON authorities(user_id);
```

---

## Pattern 2 — Custom UserDetailsService for MFA

Implement a `UserDetailsService` that loads factor metadata for `@EnableMultifactorAuthentication`.

### 2.1 — MFA-Aware UserDetails Implementation

```java
package ${basePackage}.microservices.core.<service>.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import ${basePackage}.microservices.core.<service>.persistence.AuthenticationFactor;
import java.util.Collection;
import java.util.Set;

public class FactorAwareUserDetails implements UserDetails {

    private final Long userId;
    private final String username;
    private final String email;
    private final String password;
    private final boolean enabled;
    private final boolean accountNonLocked;
    private final Collection<? extends GrantedAuthority> authorities;

    // Factor metadata
    private final boolean mfaEnabled;
    private final String mfaSecret;
    private final String webauthnCredentials;
    private final AuthenticationFactor preferredFactor;
    private final Set<String> mfaRecoveryCodes;

    public FactorAwareUserDetails(
            Long userId, String username, String email, String password,
            boolean enabled, boolean accountNonLocked,
            Collection<? extends GrantedAuthority> authorities,
            boolean mfaEnabled, String mfaSecret, String webauthnCredentials,
            AuthenticationFactor preferredFactor, Set<String> mfaRecoveryCodes) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.password = password;
        this.enabled = enabled;
        this.accountNonLocked = accountNonLocked;
        this.authorities = authorities;
        this.mfaEnabled = mfaEnabled;
        this.mfaSecret = mfaSecret;
        this.webauthnCredentials = webauthnCredentials;
        this.preferredFactor = preferredFactor;
        this.mfaRecoveryCodes = mfaRecoveryCodes;
    }

    // ── UserDetails contract ──
    @Override public String getUsername() { return username; }
    @Override public String getPassword() { return password; }
    @Override public boolean isEnabled() { return enabled; }
    @Override public boolean isAccountNonLocked() { return accountNonLocked; }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }

    // ── Factor accessors ──
    public Long getUserId() { return userId; }
    public String getEmail() { return email; }
    public boolean isMfaEnabled() { return mfaEnabled; }
    public String getMfaSecret() { return mfaSecret; }
    public String getWebauthnCredentials() { return webauthnCredentials; }
    public AuthenticationFactor getPreferredFactor() { return preferredFactor; }
    public Set<String> getMfaRecoveryCodes() { return mfaRecoveryCodes; }
}
```

### 2.2 — Custom UserDetailsService

```java
package ${basePackage}.microservices.core.<service>.security;

import ${basePackage}.microservices.core.<service>.persistence.AuthorityEntity;
import ${basePackage}.microservices.core.<service>.persistence.UserEntity;
import ${basePackage}.microservices.core.<service>.persistence.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class FactorAwareUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public FactorAwareUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserEntity user = userRepository.findByUsernameOrEmail(username, username)
            .orElseThrow(() -> new UsernameNotFoundException(
                "User not found: " + username));

        var authorities = user.getAuthorities().stream()
            .map(AuthorityEntity::getAuthority)
            .map(SimpleGrantedAuthority::new)
            .collect(Collectors.toList());

        Set<String> recoveryCodes = user.getMfaRecoveryCodes() != null
            ? Arrays.stream(user.getMfaRecoveryCodes().split(","))
                .filter(s -> !s.isBlank())
                .collect(Collectors.toSet())
            : Collections.emptySet();

        return new FactorAwareUserDetails(
            user.getId(),
            user.getUsername(),
            user.getEmail(),
            user.getPassword(),
            user.isEnabled(),
            user.isAccountNonLocked(),
            authorities,
            user.isMfaEnabled(),
            user.getMfaSecret(),
            user.getWebauthnCredentials(),
            user.getPreferredFactor(),
            recoveryCodes
        );
    }
}
```

### 2.3 — User Repository

```java
package ${basePackage}.microservices.core.<service>.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

    @EntityGraph(attributePaths = "authorities")
    Optional<UserEntity> findByUsernameOrEmail(String username, String email);

    @EntityGraph(attributePaths = "authorities")
    Optional<UserEntity> findByUsername(String username);

    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}
```

### 2.4 — Security Configuration with MFA

```java
package ${basePackage}.microservices.core.<service>.config;

import ${basePackage}.microservices.core.<service>.security.FactorAwareUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityDbConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        // Argon2id — recommended for new applications in Spring Security 7.0
        // Falls back gracefully: BCrypt hashes are auto-detected and re-hashed on login
        return Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider(
            FactorAwareUserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/**").permitAll()
                .requestMatchers("/api/auth/**").permitAll()
                .anyRequest().authenticated()
            )
            .formLogin(withDefaults())
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(withDefaults()));
        return http.build();
    }
}
```

> **Password Encoder Selection Rule:**
> - **New projects:** Use `Argon2PasswordEncoder` (memory-hard, side-channel resistant)
> - **Migrating projects:** Use `DelegatingPasswordEncoder` with `{argon2id}` as default and `{bcrypt}` for legacy hashes
> - **NEVER:** Use MD5, SHA-1, SHA-256 (unsalted), or plain text

---

## Pattern 3 — Integrated OAuth2 Database Schema

Spring Security 7.0 merges Spring Authorization Server. Configure JDBC-backed storage for clients, authorizations, and consent.

### 3.1 — Dependencies

```groovy
// build.gradle for authorization-server module
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-security'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-jdbc'
    implementation 'org.springframework.security:spring-security-oauth2-authorization-server'
    runtimeOnly 'com.mysql:mysql-connector-j'
    // Or PostgreSQL:
    // runtimeOnly 'org.postgresql:postgresql'
}
```

### 3.2 — OAuth2 Database Schema (Flyway Migration)

```sql
-- V2__oauth2_authorization_server_schema.sql
-- Standard schema from Spring Authorization Server documentation

CREATE TABLE oauth2_registered_client (
    id                            VARCHAR(100)  NOT NULL PRIMARY KEY,
    client_id                     VARCHAR(100)  NOT NULL,
    client_id_issued_at           TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    client_secret                 VARCHAR(200),
    client_secret_expires_at      TIMESTAMP,
    client_name                   VARCHAR(200)  NOT NULL,
    client_authentication_methods VARCHAR(1000) NOT NULL,
    authorization_grant_types     VARCHAR(1000) NOT NULL,
    redirect_uris                 VARCHAR(1000),
    post_logout_redirect_uris     VARCHAR(1000),
    scopes                        VARCHAR(1000) NOT NULL,
    client_settings               VARCHAR(2000) NOT NULL,
    token_settings                VARCHAR(2000) NOT NULL
);

CREATE TABLE oauth2_authorization (
    id                            VARCHAR(100)  NOT NULL PRIMARY KEY,
    registered_client_id          VARCHAR(100)  NOT NULL,
    principal_name                VARCHAR(200)  NOT NULL,
    authorization_grant_type      VARCHAR(100)  NOT NULL,
    authorized_scopes             VARCHAR(1000),
    attributes                    TEXT,
    state                         VARCHAR(500),
    authorization_code_value      TEXT,
    authorization_code_issued_at  TIMESTAMP,
    authorization_code_expires_at TIMESTAMP,
    authorization_code_metadata   TEXT,
    access_token_value            TEXT,
    access_token_issued_at        TIMESTAMP,
    access_token_expires_at       TIMESTAMP,
    access_token_metadata         TEXT,
    access_token_type             VARCHAR(100),
    access_token_scopes           VARCHAR(1000),
    oidc_id_token_value           TEXT,
    oidc_id_token_issued_at       TIMESTAMP,
    oidc_id_token_expires_at      TIMESTAMP,
    oidc_id_token_metadata        TEXT,
    refresh_token_value           TEXT,
    refresh_token_issued_at       TIMESTAMP,
    refresh_token_expires_at      TIMESTAMP,
    refresh_token_metadata        TEXT,
    user_code_value               TEXT,
    user_code_issued_at           TIMESTAMP,
    user_code_expires_at          TIMESTAMP,
    user_code_metadata            TEXT,
    device_code_value             TEXT,
    device_code_issued_at         TIMESTAMP,
    device_code_expires_at        TIMESTAMP,
    device_code_metadata          TEXT,
    CONSTRAINT fk_oauth2_auth_client
        FOREIGN KEY (registered_client_id) REFERENCES oauth2_registered_client(id)
);

CREATE TABLE oauth2_authorization_consent (
    registered_client_id VARCHAR(100)  NOT NULL,
    principal_name       VARCHAR(200)  NOT NULL,
    authorities          VARCHAR(1000) NOT NULL,
    PRIMARY KEY (registered_client_id, principal_name),
    CONSTRAINT fk_oauth2_consent_client
        FOREIGN KEY (registered_client_id) REFERENCES oauth2_registered_client(id)
);
```

### 3.3 — JDBC-Backed Authorization Server Configuration

```java
package ${basePackage}.springcloud.authorizationserver.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.JdbcRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;

@Configuration
public class AuthorizationServerDbConfig {

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http) throws Exception {
        OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http);
        http.getConfigurer(OAuth2AuthorizationServerConfigurer.class)
            .oidc(Customizer.withDefaults());
        http.exceptionHandling(ex -> ex
            .authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/login")));
        return http.build();
    }

    // ── JDBC-backed stores (replaces in-memory defaults) ──

    @Bean
    public RegisteredClientRepository registeredClientRepository(JdbcTemplate jdbcTemplate) {
        return new JdbcRegisteredClientRepository(jdbcTemplate);
    }

    @Bean
    public OAuth2AuthorizationService authorizationService(
            JdbcTemplate jdbcTemplate,
            RegisteredClientRepository registeredClientRepository) {
        return new JdbcOAuth2AuthorizationService(jdbcTemplate, registeredClientRepository);
    }

    @Bean
    public OAuth2AuthorizationConsentService authorizationConsentService(
            JdbcTemplate jdbcTemplate,
            RegisteredClientRepository registeredClientRepository) {
        return new JdbcOAuth2AuthorizationConsentService(jdbcTemplate, registeredClientRepository);
    }

    @Bean
    public AuthorizationServerSettings authorizationServerSettings() {
        return AuthorizationServerSettings.builder()
            .issuer("http://auth-server:9999")
            .build();
    }

    @Bean
    public JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
        return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
    }
}
```

### 3.4 — Registered Client Initialization (for development)

```java
package ${basePackage}.springcloud.authorizationserver.config;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;

import java.time.Duration;
import java.util.UUID;

@Configuration
@Profile("!prod")  // NEVER run in production — use Flyway seed or admin API
public class DevClientInitializer {

    @Bean
    public ApplicationRunner registerDevClients(
            RegisteredClientRepository repo, PasswordEncoder encoder) {
        return args -> {
            if (repo.findByClientId("reader") == null) {
                RegisteredClient reader = RegisteredClient.withId(UUID.randomUUID().toString())
                    .clientId("reader")
                    .clientSecret(encoder.encode("secret"))
                    .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                    .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                    .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                    .redirectUri("https://my.redirect.uri")
                    .redirectUri("https://localhost:8443/webjars/swagger-ui/oauth2-redirect.html")
                    .scope(OidcScopes.OPENID)
                    .scope("product:read")
                    .clientSettings(ClientSettings.builder()
                        .requireAuthorizationConsent(true).build())
                    .tokenSettings(TokenSettings.builder()
                        .accessTokenTimeToLive(Duration.ofHours(1))
                        .refreshTokenTimeToLive(Duration.ofHours(24)).build())
                    .build();
                repo.save(reader);
            }

            if (repo.findByClientId("writer") == null) {
                RegisteredClient writer = RegisteredClient.withId(UUID.randomUUID().toString())
                    .clientId("writer")
                    .clientSecret(encoder.encode("secret"))
                    .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                    .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                    .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                    .redirectUri("https://my.redirect.uri")
                    .redirectUri("https://localhost:8443/webjars/swagger-ui/oauth2-redirect.html")
                    .scope(OidcScopes.OPENID)
                    .scope("product:read")
                    .scope("product:write")
                    .clientSettings(ClientSettings.builder()
                        .requireAuthorizationConsent(true).build())
                    .tokenSettings(TokenSettings.builder()
                        .accessTokenTimeToLive(Duration.ofHours(1))
                        .refreshTokenTimeToLive(Duration.ofHours(24)).build())
                    .build();
                repo.save(writer);
            }
        };
    }
}
```

---

## Pattern 4 — JdbcUserDetailsManager Customization

Extend `JdbcUserDetailsManager` for email-based login and factor-related fields.

### 4.1 — Extended JdbcUserDetailsManager

```java
package ${basePackage}.microservices.core.<service>.security;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.provisioning.JdbcUserDetailsManager;

import javax.sql.DataSource;

/**
 * Extends JdbcUserDetailsManager to:
 * 1. Support email-based login (username OR email match)
 * 2. Load MFA factor metadata alongside UserDetails
 * 3. Track failed login attempts for account lockout
 */
public class ExtendedJdbcUserDetailsManager extends JdbcUserDetailsManager {

    private static final String USERS_BY_USERNAME_OR_EMAIL_QUERY =
        "SELECT username, password, enabled FROM users WHERE username = ? OR email = ?";

    private static final String AUTHORITIES_BY_USERNAME_QUERY =
        "SELECT u.username, a.authority FROM authorities a " +
        "INNER JOIN users u ON a.user_id = u.id WHERE u.username = ? OR u.email = ?";

    private static final String INCREMENT_FAILED_ATTEMPTS =
        "UPDATE users SET failed_login_attempts = failed_login_attempts + 1 WHERE username = ?";

    private static final String RESET_FAILED_ATTEMPTS =
        "UPDATE users SET failed_login_attempts = 0, last_login_at = CURRENT_TIMESTAMP WHERE username = ?";

    private static final String LOCK_ACCOUNT =
        "UPDATE users SET account_non_locked = FALSE, lockout_until = ? WHERE username = ?";

    private final JdbcTemplate jdbcTemplate;

    public ExtendedJdbcUserDetailsManager(DataSource dataSource) {
        super(dataSource);
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        setUsersByUsernameQuery(USERS_BY_USERNAME_OR_EMAIL_QUERY);
        setAuthoritiesByUsernameQuery(AUTHORITIES_BY_USERNAME_QUERY);
    }

    @Override
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        // identifier can be username or email — both queries accept it
        return super.loadUserByUsername(identifier);
    }

    public void incrementFailedAttempts(String username) {
        jdbcTemplate.update(INCREMENT_FAILED_ATTEMPTS, username);
    }

    public void resetFailedAttempts(String username) {
        jdbcTemplate.update(RESET_FAILED_ATTEMPTS, username);
    }

    public void lockAccount(String username, java.time.Instant lockoutUntil) {
        jdbcTemplate.update(LOCK_ACCOUNT,
            java.sql.Timestamp.from(lockoutUntil), username);
    }
}
```

### 4.2 — Account Lockout Listener

```java
package ${basePackage}.microservices.core.<service>.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
public class AccountLockoutListener {

    private static final Logger LOG = LoggerFactory.getLogger(AccountLockoutListener.class);
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCKOUT_MINUTES = 30;

    private final ExtendedJdbcUserDetailsManager userDetailsManager;

    public AccountLockoutListener(ExtendedJdbcUserDetailsManager userDetailsManager) {
        this.userDetailsManager = userDetailsManager;
    }

    @EventListener
    public void onAuthenticationFailure(AuthenticationFailureBadCredentialsEvent event) {
        String username = event.getAuthentication().getName();
        userDetailsManager.incrementFailedAttempts(username);
        LOG.warn("Failed login attempt for user: {}", username);

        // Lock account after MAX_FAILED_ATTEMPTS
        // (actual count check should query DB — simplified here)
    }

    @EventListener
    public void onAuthenticationSuccess(AuthenticationSuccessEvent event) {
        String username = event.getAuthentication().getName();
        userDetailsManager.resetFailedAttempts(username);
    }
}
```

---

## Pattern 5 — One-Time Token (OTT) Persistence

Implement `OneTimeTokenService` with database backend for passwordless / magic-link logins.

### 5.1 — OTT Database Table

```sql
-- V3__one_time_token_schema.sql

CREATE TABLE one_time_tokens (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    token_value VARCHAR(255) NOT NULL UNIQUE,
    username    VARCHAR(50)  NOT NULL,
    issued_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at  TIMESTAMP    NOT NULL,
    used        BOOLEAN      NOT NULL DEFAULT FALSE,
    used_at     TIMESTAMP,
    CONSTRAINT fk_ott_user FOREIGN KEY (username) REFERENCES users(username)
);

CREATE INDEX idx_ott_token ON one_time_tokens(token_value);
CREATE INDEX idx_ott_username ON one_time_tokens(username);
CREATE INDEX idx_ott_expires ON one_time_tokens(expires_at);
```

### 5.2 — OTT Entity

```java
package ${basePackage}.microservices.core.<service>.persistence;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "one_time_tokens")
public class OneTimeTokenEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "token_value", nullable = false, unique = true)
    private String tokenValue;

    @Column(nullable = false, length = 50)
    private String username;

    @Column(name = "issued_at", nullable = false, updatable = false)
    private Instant issuedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private boolean used = false;

    @Column(name = "used_at")
    private Instant usedAt;

    // Getters + Setters
}
```

### 5.3 — OTT Repository

```java
package ${basePackage}.microservices.core.<service>.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import java.time.Instant;
import java.util.Optional;

public interface OneTimeTokenRepository extends JpaRepository<OneTimeTokenEntity, Long> {

    Optional<OneTimeTokenEntity> findByTokenValueAndUsedFalse(String tokenValue);

    @Modifying
    @Query("DELETE FROM OneTimeTokenEntity t WHERE t.expiresAt < :now")
    int deleteExpiredTokens(Instant now);
}
```

### 5.4 — OneTimeTokenService Implementation

```java
package ${basePackage}.microservices.core.<service>.security;

import ${basePackage}.microservices.core.<service>.persistence.OneTimeTokenEntity;
import ${basePackage}.microservices.core.<service>.persistence.OneTimeTokenRepository;
import org.springframework.security.authentication.ott.GenerateOneTimeTokenRequest;
import org.springframework.security.authentication.ott.OneTimeToken;
import org.springframework.security.authentication.ott.OneTimeTokenService;
import org.springframework.security.authentication.ott.DefaultOneTimeToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class JdbcOneTimeTokenService implements OneTimeTokenService {

    private static final int TOKEN_VALIDITY_MINUTES = 15;

    private final OneTimeTokenRepository tokenRepository;

    public JdbcOneTimeTokenService(OneTimeTokenRepository tokenRepository) {
        this.tokenRepository = tokenRepository;
    }

    @Override
    @Transactional
    public OneTimeToken generate(GenerateOneTimeTokenRequest request) {
        String tokenValue = UUID.randomUUID().toString();
        Instant now = Instant.now();
        Instant expiresAt = now.plus(TOKEN_VALIDITY_MINUTES, ChronoUnit.MINUTES);

        OneTimeTokenEntity entity = new OneTimeTokenEntity();
        entity.setTokenValue(tokenValue);
        entity.setUsername(request.getUsername());
        entity.setIssuedAt(now);
        entity.setExpiresAt(expiresAt);
        entity.setUsed(false);
        tokenRepository.save(entity);

        return new DefaultOneTimeToken(tokenValue, request.getUsername(), expiresAt);
    }

    @Override
    @Transactional
    public OneTimeToken consume(String tokenValue) {
        OneTimeTokenEntity entity = tokenRepository
            .findByTokenValueAndUsedFalse(tokenValue)
            .orElse(null);

        if (entity == null || entity.getExpiresAt().isBefore(Instant.now())) {
            return null;  // Invalid or expired
        }

        entity.setUsed(true);
        entity.setUsedAt(Instant.now());
        tokenRepository.save(entity);

        return new DefaultOneTimeToken(
            entity.getTokenValue(), entity.getUsername(), entity.getExpiresAt());
    }

    /**
     * Scheduled cleanup — call from @Scheduled or externally.
     */
    @Transactional
    public int purgeExpiredTokens() {
        return tokenRepository.deleteExpiredTokens(Instant.now());
    }
}
```

### 5.5 — OTT Security Configuration

```java
// Add to SecurityDbConfig or a dedicated OTT config
@Bean
public SecurityFilterChain ottSecurityFilterChain(
        HttpSecurity http, OneTimeTokenService ottService) throws Exception {
    http
        .oneTimeTokenLogin(ott -> ott
            .oneTimeTokenService(ottService)
            .tokenGeneratingUrl("/api/auth/ott/generate")
            .loginProcessingUrl("/api/auth/ott/login")
            .defaultSuccessUrl("/", true)
        );
    return http.build();
}
```

---

## Pattern 6 — Session Repository (Distributed Sessions)

Move session management to the database for distributed session limits via Spring Session JDBC.

### 6.1 — Dependencies

```groovy
dependencies {
    implementation 'org.springframework.session:spring-session-jdbc'
    implementation 'org.springframework.boot:spring-boot-starter-jdbc'
}
```

### 6.2 — Configuration

```yaml
# application.yml
spring:
  session:
    store-type: jdbc
    jdbc:
      initialize-schema: always   # auto-creates SPRING_SESSION tables
      table-name: SPRING_SESSION
    timeout: 30m
  # Optional: limit concurrent sessions
  security:
    sessions:
      maximum: 3   # max 3 concurrent sessions per user
```

### 6.3 — Session Tables (auto-created or Flyway)

```sql
-- V4__spring_session_schema.sql (optional — Spring Session can auto-create)

CREATE TABLE SPRING_SESSION (
    PRIMARY_ID            CHAR(36) NOT NULL PRIMARY KEY,
    SESSION_ID            CHAR(36) NOT NULL,
    CREATION_TIME         BIGINT   NOT NULL,
    LAST_ACCESS_TIME      BIGINT   NOT NULL,
    MAX_INACTIVE_INTERVAL INT      NOT NULL,
    EXPIRY_TIME           BIGINT   NOT NULL,
    PRINCIPAL_NAME        VARCHAR(100)
);

CREATE UNIQUE INDEX SPRING_SESSION_IX1 ON SPRING_SESSION(SESSION_ID);
CREATE INDEX SPRING_SESSION_IX2 ON SPRING_SESSION(EXPIRY_TIME);
CREATE INDEX SPRING_SESSION_IX3 ON SPRING_SESSION(PRINCIPAL_NAME);

CREATE TABLE SPRING_SESSION_ATTRIBUTES (
    SESSION_PRIMARY_ID CHAR(36)     NOT NULL,
    ATTRIBUTE_NAME     VARCHAR(200) NOT NULL,
    ATTRIBUTE_BYTES    BLOB         NOT NULL,
    PRIMARY KEY (SESSION_PRIMARY_ID, ATTRIBUTE_NAME),
    CONSTRAINT SPRING_SESSION_ATTRIBUTES_FK
        FOREIGN KEY (SESSION_PRIMARY_ID) REFERENCES SPRING_SESSION(PRIMARY_ID) ON DELETE CASCADE
);
```

### 6.4 — Concurrent Session Control

```java
@Bean
public SecurityFilterChain sessionFilterChain(HttpSecurity http) throws Exception {
    http
        .sessionManagement(session -> session
            .maximumSessions(3)
            .expiredUrl("/login?expired")
        );
    return http.build();
}
```

---

## Pattern 7 — Security Audit Logging

Enable database auditing for security-sensitive entities using Spring Data JPA auditing.

### 7.1 — Enable JPA Auditing

```java
package ${basePackage}.microservices.core.<service>.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

@Configuration
@EnableJpaAuditing(auditorAwareRef = "securityAuditorAware")
public class AuditConfig {

    @Bean
    public AuditorAware<String> securityAuditorAware() {
        return () -> {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
                return Optional.of("SYSTEM");
            }
            return Optional.of(auth.getName());
        };
    }
}
```

### 7.2 — Auditable Base Entity

```java
package ${basePackage}.microservices.core.<service>.persistence;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class AuditableEntity {

    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private String createdBy;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @LastModifiedBy
    @Column(name = "modified_by")
    private String modifiedBy;

    @LastModifiedDate
    @Column(name = "modified_at")
    private Instant modifiedAt;

    // Getters + Setters
    public String getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public String getModifiedBy() { return modifiedBy; }
    public Instant getModifiedAt() { return modifiedAt; }
}
```

### 7.3 — Security Audit Event Table

```sql
-- V5__security_audit_log.sql

CREATE TABLE security_audit_log (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_type     VARCHAR(50)  NOT NULL,  -- LOGIN_SUCCESS, LOGIN_FAILURE, LOGOUT, TOKEN_ISSUED, MFA_CHALLENGE, etc.
    principal_name VARCHAR(100),
    ip_address     VARCHAR(45),
    user_agent     VARCHAR(500),
    details        TEXT,
    created_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_principal ON security_audit_log(principal_name);
CREATE INDEX idx_audit_event ON security_audit_log(event_type);
CREATE INDEX idx_audit_time ON security_audit_log(created_at);
```

### 7.4 — Security Event Listener

```java
package ${basePackage}.microservices.core.<service>.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.event.*;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.stereotype.Component;

@Component
public class SecurityAuditEventListener {

    private static final Logger LOG = LoggerFactory.getLogger(SecurityAuditEventListener.class);

    private final JdbcTemplate jdbcTemplate;

    public SecurityAuditEventListener(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @EventListener
    public void onSuccess(AuthenticationSuccessEvent event) {
        logAuditEvent("LOGIN_SUCCESS", event.getAuthentication().getName(),
            extractIp(event.getAuthentication().getDetails()));
    }

    @EventListener
    public void onFailure(AbstractAuthenticationFailureEvent event) {
        logAuditEvent("LOGIN_FAILURE", event.getAuthentication().getName(),
            extractIp(event.getAuthentication().getDetails()));
    }

    @EventListener
    public void onLogout(org.springframework.security.authentication.event.LogoutSuccessEvent event) {
        logAuditEvent("LOGOUT", event.getAuthentication().getName(), null);
    }

    private void logAuditEvent(String eventType, String principal, String ip) {
        // Use parameterized query — NEVER concatenate user input
        jdbcTemplate.update(
            "INSERT INTO security_audit_log (event_type, principal_name, ip_address, created_at) VALUES (?, ?, ?, CURRENT_TIMESTAMP)",
            eventType, principal, ip);
        LOG.info("Security audit: {} for {} from {}", eventType, principal, ip);
    }

    private String extractIp(Object details) {
        if (details instanceof WebAuthenticationDetails wad) {
            return wad.getRemoteAddress();
        }
        return null;
    }
}
```

---

## Security Best Practices Checklist

These are **non-negotiable** rules for all security-db patterns:

| # | Rule | Enforcement |
|---|------|-------------|
| 1 | **Parameterized queries only** — NEVER concatenate user input into SQL | Use Spring Data JPA `@Query` with `?1` / `:param` or `JdbcTemplate.update(sql, args)` |
| 2 | **Password hashing** — Argon2id for new, BCrypt acceptable for legacy | `Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8()` or `BCryptPasswordEncoder` |
| 3 | **Secrets encryption at rest** — MFA secrets, OAuth client secrets | Use `{cipher}` with Spring Cloud Config encryption or JCE column-level encryption |
| 4 | **Audit trail** — All auth events logged to `security_audit_log` | `SecurityAuditEventListener` bean registered |
| 5 | **Account lockout** — Lock after N failed attempts | `AccountLockoutListener` + `failed_login_attempts` column |
| 6 | **Token expiry** — OTT tokens expire in ≤ 15 min | `expires_at` column + `purgeExpiredTokens()` scheduled |
| 7 | **No plaintext secrets** — Client secrets, token values stored as hashes | `PasswordEncoder.encode()` for client secrets |
| 8 | **Session limits** — Max concurrent sessions configured | `spring.session.timeout` + `maximumSessions()` |
| 9 | **SQL injection prevention** — No raw `String.format` or `+` in queries | Governance gate: grep for `"SELECT.*" +` or `String.format.*SELECT` patterns |
| 10 | **WebAuthn credential rotation** — Stale credentials flagged | `webauthn_credentials` JSON includes `createdAt` per credential |

---

## Docker Compose Integration

Add MySQL for security tables alongside existing infrastructure:

```yaml
# Additions to compose.yaml
auth-db:
  image: mysql:8.4
  mem_limit: 512m
  environment:
    MYSQL_ROOT_PASSWORD: rootpwd
    MYSQL_DATABASE: auth-db
    MYSQL_USER: auth-user
    MYSQL_PASSWORD: auth-pwd
  healthcheck:
    test: "/usr/bin/mysql --user=auth-user --password=auth-pwd --execute \"SHOW DATABASES;\""
    interval: 5s
    timeout: 2s
    retries: 60
  ports:
    - "3307:3306"

authorization-server:
  build: spring-cloud/authorization-server
  mem_limit: 512m
  environment:
    - SPRING_PROFILES_ACTIVE=docker
    - SPRING_DATASOURCE_URL=jdbc:mysql://auth-db:3306/auth-db
    - SPRING_DATASOURCE_USERNAME=auth-user
    - SPRING_DATASOURCE_PASSWORD=auth-pwd
  depends_on:
    auth-db:
      condition: service_healthy
```

### application.yml for authorization-server (docker profile)

```yaml
spring:
  datasource:
    url: jdbc:mysql://auth-db:3306/auth-db
    username: auth-user
    password: auth-pwd
    driver-class-name: com.mysql.cj.jdbc.Driver
  jpa:
    hibernate:
      ddl-auto: validate   # Flyway manages schema
    show-sql: false
  flyway:
    enabled: true
    locations: classpath:db/migration
```

---

## Verification Checklist

After applying any security-db pattern, verify these items (use [verify](../verify/verify.md) sub-skill):

| # | Check | Command / Method |
|---|-------|-----------------|
| 1 | Schema migration runs | `./gradlew :spring-cloud:authorization-server:flywayMigrate` |
| 2 | UserDetailsService loads with factors | Unit test: load user → assert `isMfaEnabled()`, `getMfaSecret()` |
| 3 | OAuth2 JDBC stores functional | Integration test: register client → authorize → issue token → verify from DB |
| 4 | OTT generate + consume lifecycle | Unit test: generate → consume → assert `used=true` → consume again → assert null |
| 5 | Account lockout triggers | Test: N failed logins → assert `accountNonLocked=false` |
| 6 | Password encoding consistent | Test: encode → matches → verify `{argon2id}` or `{bcrypt}` prefix |
| 7 | Audit log captures events | Integration test: login → query `security_audit_log` → assert row exists |
| 8 | No SQL injection vectors | `grep -rn "\"SELECT\|\"INSERT\|\"UPDATE\|\"DELETE" --include="*.java" \| grep -v "@Query\|JdbcTemplate"` |
| 9 | Expired OTT cleanup | Test: create expired token → `purgeExpiredTokens()` → assert deleted |
| 10 | Session table initialized | `SELECT COUNT(*) FROM SPRING_SESSION` after login |
| 11 | Compilation passes | `./gradlew compileJava` on all affected modules |
| 12 | Full build green | `./gradlew build` |

---

## Key Components Reference

| Component | Responsibility | Pattern Reference |
|-----------|---------------|-------------------|
| `UserDetailsService` | Fetches user factor data (password, OTT secrets, Passkey IDs) from the DB | Pattern 2 |
| `PasswordEncoder` | BCrypt or Argon2 hashing for DB credentials | Pattern 2 (§2.4) |
| `JdbcTemplate` / JPA | Custom repository logic for factor-based authorization rules | Patterns 1, 4, 5 |
| `SessionRepository` | Moves session management to DB for distributed session limits | Pattern 6 |
| `RegisteredClientRepository` | JDBC-backed OAuth2 client storage | Pattern 3 |
| `OAuth2AuthorizationService` | JDBC-backed token + authorization state storage | Pattern 3 |
| `OneTimeTokenService` | Database-backed single-use token generation and validation | Pattern 5 |
| `AuditorAware` | Populates `@CreatedBy` / `@LastModifiedBy` from SecurityContext | Pattern 7 |
