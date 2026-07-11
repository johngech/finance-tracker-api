package com.marakicode.financetracker.users;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "User role", enumAsRef = true)
public enum Role {
    USER,
    ADMIN
}
