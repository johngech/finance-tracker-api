package com.marakicode.financetracker.common;

public final class ValidationConstants {

    private ValidationConstants() {}

    public static final String EMAIL_REGEX =
            "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";

    public static final String EMAIL_MESSAGE =
            "Email must be valid (e.g., john@domain.com)";

    public static final String PASSWORD_REGEX =
            "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!]).+$";

    public static final String PASSWORD_MESSAGE =
            "Password must contain at least one digit, one lowercase, one uppercase, and one special character (@#$%^&+=!)";
}
