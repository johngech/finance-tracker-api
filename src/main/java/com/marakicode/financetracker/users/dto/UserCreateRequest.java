package com.marakicode.financetracker.users.dto;

import com.marakicode.financetracker.common.ValidationConstants;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

@Schema(description = "Request payload for creating a new user")
public record UserCreateRequest(
    @Schema(example = "John", description = "User first name (2-50 characters)")
    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
    String firstName,

    @Schema(example = "Doe", description = "User last name (2-50 characters)")
    @NotBlank(message = "Last name is required")
    @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
    String lastName,

    @Schema(example = "user@example.com", description = "User email address")
    @NotBlank(message = "Email is required")
    @Pattern(
        regexp = ValidationConstants.EMAIL_REGEX,
        message = ValidationConstants.EMAIL_MESSAGE
    )
    String email,

    @Schema(example = "P@ssw0rd!", description = "User password (8-100 characters, must contain at least one uppercase letter, one lowercase letter, and one digit)")
    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
    @Pattern(
        regexp = ValidationConstants.PASSWORD_REGEX,
        message = ValidationConstants.PASSWORD_MESSAGE
    )
    String password
) {}
