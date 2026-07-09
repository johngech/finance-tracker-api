package com.marakicode.financetracker.common;

import java.time.LocalDateTime;
import java.util.List;

public record ErrorDto(
    LocalDateTime timestamp,
    int status,
    String error,
    String message,
    String path,
    List<FieldError> fieldErrors
) {

    public record FieldError(String field, String message) {}

    public static ErrorDto of(int status, String error, String message, String path) {
        return new ErrorDto(LocalDateTime.now(), status, error, message, path, List.of());
    }

    public static ErrorDto of(int status, String error, String message, String path, List<FieldError> fieldErrors) {
        return new ErrorDto(LocalDateTime.now(), status, error, message, path, fieldErrors);
    }
}
