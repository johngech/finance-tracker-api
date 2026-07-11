package com.marakicode.financetracker.common;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Standard API response wrapper")
public record ApiResponse<T>(
    @Schema(description = "Indicates whether the request was successful", example = "true")
    boolean success,

    @Schema(description = "Response message", example = "Operation completed successfully")
    String message,

    @Schema(description = "Response payload")
    T data
) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, "Success", data);
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data);
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, message, null);
    }
}