package com.marakicode.financetracker.auth;

import com.marakicode.financetracker.common.ValidationConstants;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record LoginRequest(
    @NotBlank(message = "Email is required")
    @Pattern(
        regexp = ValidationConstants.EMAIL_REGEX,
        message = ValidationConstants.EMAIL_MESSAGE
    )
    String email,

    @NotBlank(message = "Password is required")
    String password
) {}
