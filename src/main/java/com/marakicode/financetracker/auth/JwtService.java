package com.marakicode.financetracker.auth;

import com.marakicode.financetracker.users.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Date;

@AllArgsConstructor
@Service
public class JwtService {
    private final JwtConfig jwtConfig;

    public Jwt generateAccessToken(User user) {
        return generateToken(user, jwtConfig.getAccessTokenExpiration());
    }

    public Jwt generateRefreshToken(User user) {
        return generateToken(user, jwtConfig.getRefreshTokenExpiration());
    }

    private Jwt generateToken(User user, long tokenExpiration) {
        var claims = Jwts.claims()
                .subject(user.getId().toString())
                .add("email", user.getEmail())
                .add("role", user.getRole().name())
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

    public Jwt parseToken(String token) {
        try {
            return new Jwt(jwtConfig.getSecretKey(), getClaims(token));
        } catch (JwtException ex) {
            return null;
        }
    }

    public long getAccessTokenExpiration() {
        return jwtConfig.getAccessTokenExpiration();
    }

    public long getRefreshTokenExpiration() {
        return jwtConfig.getRefreshTokenExpiration();
    }
}
