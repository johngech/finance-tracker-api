package com.marakicode.financetracker.common;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "Error response body")
public record ErrorDto(
    @Schema(description = "Timestamp when the error occurred")
    LocalDateTime timestamp,

    @Schema(description = "HTTP status code", example = "400")
    int status,

    @Schema(description = "Error category", example = "Validation Failed")
    String error,

    @Schema(description = "Human-readable error description", example = "Request validation failed")
    String message,

    @Schema(description = "Request URI that caused the error", example = "/api/v1/users")
    String path,

    @Schema(description = "List of field-level validation errors (if applicable)")
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
