package com.marakicode.financetracker.common;

import java.util.Objects;

/**
 * Utility for search-related string operations, such as escaping LIKE
 * wildcards in user-supplied input to prevent unintended pattern matching.
 */
public final class SearchUtils {

    private static final String LIKE_ESCAPE_CHAR = "\\";

    private SearchUtils() {
    }

    /**
     * Escapes the LIKE wildcard characters {@code %}, {@code _} and the escape
     * character itself ({@code \}) in the given input, then wraps it in
     * {@code %...%} for a contains-style pattern.
     *
     * @param input the raw user-supplied search term (must not be null)
     * @return a LIKE-safe contains pattern, e.g. {@code %search\%20term%}
     */
    public static String toLikeContainsPattern(String input) {
        return "%" + escapeLike(input) + "%";
    }

    /**
     * Escapes the LIKE wildcard characters in the given input.
     */
    public static String escapeLike(String input) {
        Objects.requireNonNull(input, "input must not be null");
        return input
                .replace(LIKE_ESCAPE_CHAR, LIKE_ESCAPE_CHAR + LIKE_ESCAPE_CHAR)
                .replace("%", LIKE_ESCAPE_CHAR + "%")
                .replace("_", LIKE_ESCAPE_CHAR + "_");
    }
}
