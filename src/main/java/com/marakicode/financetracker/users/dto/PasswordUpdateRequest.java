package com.marakicode.financetracker.users.dto;

import com.marakicode.financetracker.common.ValidationConstants;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Request payload for changing a user's password")
public record PasswordUpdateRequest(
    @Schema(example = "OldP@ss1", description = "Current password for verification")
    @NotBlank(message = "Current password is required")
    String oldPassword,

    @Schema(example = "NewP@ss1", description = "New password (8-100 characters, must contain at least one uppercase letter, one lowercase letter, and one digit)")
    @NotBlank(message = "New password is required")
    @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
    @Pattern(
        regexp = ValidationConstants.PASSWORD_REGEX,
        message = ValidationConstants.PASSWORD_MESSAGE
    )
    String newPassword
) {}
