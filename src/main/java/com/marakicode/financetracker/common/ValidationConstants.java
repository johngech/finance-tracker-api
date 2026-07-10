package com.marakicode.financetracker.common;

public final class ValidationConstants {

    private ValidationConstants() {}

    public static final String PASSWORD_REGEX =
            "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!]).+$";

    public static final String PASSWORD_MESSAGE =
            "Password must contain at least one digit, one lowercase, one uppercase, and one special character (@#$%^&+=!)";
}
