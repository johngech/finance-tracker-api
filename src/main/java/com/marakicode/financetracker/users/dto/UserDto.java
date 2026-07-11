package com.marakicode.financetracker.users.dto;

import com.marakicode.financetracker.users.Role;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "User profile information")
public record UserDto(
    @Schema(example = "1", description = "Unique user identifier")
    Long id,

    @Schema(example = "John", description = "User first name")
    String firstName,

    @Schema(example = "Doe", description = "User last name")
    String lastName,

    @Schema(example = "user@example.com", description = "User email address")
    String email,

    @Schema(description = "User role", example = "USER")
    Role role,

    @Schema(description = "Account creation timestamp")
    LocalDateTime createdAt
) {}
