package com.marakicode.financetracker.auth;

import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Configuration
@ConfigurationProperties(prefix = "spring.jwt")
@Getter
@Setter
public class JwtConfig {
    @ToString.Exclude
    private String secret;
    private long accessTokenExpiration;
    private long refreshTokenExpiration;

    private volatile SecretKey cachedSecretKey;

    /**
     * Returns the cached SecretKey, computing it lazily on first access.
     * Thread-safe via double-checked locking. Uses UTF-8 encoding for
     * consistent key derivation across environments.
     */
    public SecretKey getSecretKey() {
        SecretKey key = cachedSecretKey;
        if (key == null) {
            synchronized (this) {
                key = cachedSecretKey;
                if (key == null) {
                    key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
                    cachedSecretKey = key;
                }
            }
        }
        return key;
    }
}
