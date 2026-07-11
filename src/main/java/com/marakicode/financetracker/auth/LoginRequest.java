package com.marakicode.financetracker.auth;

import com.marakicode.financetracker.common.ValidationConstants;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "Login request payload")
public record LoginRequest(
    @Schema(example = "user@example.com", description = "User email address")
    @NotBlank(message = "Email is required")
    @Pattern(
        regexp = ValidationConstants.EMAIL_REGEX,
        message = ValidationConstants.EMAIL_MESSAGE
    )
    String email,

    @Schema(example = "P@ssw0rd!", description = "User password")
    @NotBlank(message = "Password is required")
    String password
) {}
