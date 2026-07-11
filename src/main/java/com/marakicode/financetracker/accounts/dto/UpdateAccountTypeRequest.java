package com.marakicode.financetracker.accounts.dto;

import com.marakicode.financetracker.accounts.AccountType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request payload for updating account type")
public record UpdateAccountTypeRequest(
    @Schema(description = "New account type")
    @NotNull(message = "Account type is required")
    AccountType type
) {}
