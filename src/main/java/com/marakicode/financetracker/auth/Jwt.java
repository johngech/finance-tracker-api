package com.marakicode.financetracker.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.AllArgsConstructor;

import javax.crypto.SecretKey;
import java.util.Date;

@AllArgsConstructor
public class Jwt {
    private final SecretKey secretKey;
    private final Claims claims;

    public static final String TYPE_ACCESS = "access";
    public static final String TYPE_REFRESH = "refresh";

    public boolean isExpired() {
        return claims.getExpiration().before(new Date());
    }

    public Long getUserId() {
        return Long.valueOf(claims.getSubject());
    }

    public String getRole() {
        return claims.get("role", String.class);
    }

    /**
     * Returns the token type claim ("access" or "refresh").
     * Tokens without a type claim are treated as access tokens for backward compatibility.
     */
    public String getType() {
        String type = claims.get("type", String.class);
        return type != null ? type : TYPE_ACCESS;
    }

    public boolean isRefreshToken() {
        return TYPE_REFRESH.equals(getType());
    }

    @Override
    public String toString() {
        return Jwts.builder()
                .claims(claims)
                .signWith(secretKey)
                .compact();
    }
}
