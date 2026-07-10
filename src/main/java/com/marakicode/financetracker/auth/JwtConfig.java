package com.marakicode.financetracker.auth;

import io.jsonwebtoken.security.Keys;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import javax.crypto.SecretKey;

@Configuration
@ConfigurationProperties(prefix = "spring.jwt")
@Data
public class JwtConfig {
    private String secret;
    private long accessTokenExpiration;
    private long refreshTokenExpiration;

    @Setter(AccessLevel.NONE)
    private SecretKey cachedSecretKey;

    public SecretKey getSecretKey() {
        if (cachedSecretKey == null) {
            cachedSecretKey = Keys.hmacShaKeyFor(secret.getBytes());
        }
        return cachedSecretKey;
    }
}
