package com.marakicode.financetracker.common;

import java.util.Objects;

public final class EmailNormalizer {

    private EmailNormalizer() {}

    public static String normalize(String email) {
        Objects.requireNonNull(email, "email must not be null");
        return email.toLowerCase().trim();
    }
}
