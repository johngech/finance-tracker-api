package com.marakicode.financetracker.users.dto;

import com.marakicode.financetracker.users.Role;

import java.time.LocalDateTime;

public record UserDto(
    Long id,
    String firstName,
    String lastName,
    String email,
    Role role,
    LocalDateTime createdAt
) {}
