package com.marakicode.financetracker.users.dto;

import com.marakicode.financetracker.users.Role;

import java.time.LocalDateTime;

/**
 * Admin-facing user summary. Exposes no internal implementation details.
 */
public record UserSummary(
    Long id,
    String firstName,
    String lastName,
    String email,
    Role role,
    boolean active,
    LocalDateTime createdAt
) {}
