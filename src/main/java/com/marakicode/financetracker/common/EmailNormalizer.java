package com.marakicode.financetracker.common;

public final class EmailNormalizer {

    private EmailNormalizer() {}

    public static String normalize(String email) {
        return email.toLowerCase().trim();
    }
}
