package com.marakicode.financetracker.auth;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "JWT access token response")
public record JwtResponse(
    @Schema(example = "eyJhbGciOiJIUzI1NiJ9...", description = "JWT access token (Bearer token)")
    String accessToken
) {}
