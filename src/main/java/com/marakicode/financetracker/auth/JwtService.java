package com.marakicode.financetracker.auth;

import com.marakicode.financetracker.users.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Optional;

@Slf4j
@AllArgsConstructor
@Service
public class JwtService {
    private final JwtConfig jwtConfig;

    public Jwt generateAccessToken(User user) {
        return generateToken(user, jwtConfig.getAccessTokenExpiration(), Jwt.TYPE_ACCESS);
    }

    public Jwt generateRefreshToken(User user) {
        return generateToken(user, jwtConfig.getRefreshTokenExpiration(), Jwt.TYPE_REFRESH);
    }

    private Jwt generateToken(User user, long tokenExpiration, String type) {
        var claims = Jwts.claims()
                .subject(user.getId().toString())
                .add("email", user.getEmail())
                .add("role", user.getRole().name())
                .add("type", type)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + tokenExpiration * 1000))
                .build();
        return new Jwt(jwtConfig.getSecretKey(), claims);
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(jwtConfig.getSecretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Parses and validates a JWT token string.
     *
     * @return an {@link Optional} containing the parsed {@link Jwt}, or empty if the token
     *         is invalid, malformed, or cannot be verified.
     */
    public Optional<Jwt> parseToken(String token) {
        try {
            var jwt = new Jwt(jwtConfig.getSecretKey(), getClaims(token));
            log.debug("event=jwt.parse_success userId={}", jwt.getUserId());
            return Optional.of(jwt);
        } catch (JwtException ex) {
            log.debug("event=jwt.parse_failed reason={}", ex.getMessage());
            return Optional.empty();
        }
    }

    public long getAccessTokenExpiration() {
        return jwtConfig.getAccessTokenExpiration();
    }

    public long getRefreshTokenExpiration() {
        return jwtConfig.getRefreshTokenExpiration();
    }
}
