package com.marakicode.financetracker.admin.dto;

import com.marakicode.financetracker.users.Role;
import jakarta.validation.constraints.NotNull;

public record RoleUpdateRequest(
    @NotNull(message = "Role is required")
    Role role
) {}
