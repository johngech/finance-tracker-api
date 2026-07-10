package com.marakicode.financetracker.auth;

import org.springframework.lang.NonNull;

import java.io.Serializable;

/**
 * Custom principal stored in the SecurityContext that holds both the user's
 * database ID and email. This allows {@code @PreAuthorize} expressions to
 * check ownership by ID without a database lookup.
 */
public record UserIdPrincipal(Long id, String email) implements Serializable {

    @Override
    @NonNull
    public String toString() {
        return email;
    }
}
