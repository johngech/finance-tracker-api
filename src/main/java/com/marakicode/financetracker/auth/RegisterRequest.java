package com.marakicode.financetracker.auth;

import com.marakicode.financetracker.common.ValidationConstants;
import jakarta.validation.constraints.*;

public record RegisterRequest(
    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
    String firstName,

    @NotBlank(message = "Last name is required")
    @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
    String lastName,

    @NotBlank(message = "Email is required")
    @Pattern(
        regexp = ValidationConstants.EMAIL_REGEX,
        message = ValidationConstants.EMAIL_MESSAGE
    )
    String email,

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
    @Pattern(
        regexp = ValidationConstants.PASSWORD_REGEX,
        message = ValidationConstants.PASSWORD_MESSAGE
    )
    String password
) {}
