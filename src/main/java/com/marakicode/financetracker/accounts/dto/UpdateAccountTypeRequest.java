package com.marakicode.financetracker.accounts.dto;

import com.marakicode.financetracker.accounts.AccountType;
import jakarta.validation.constraints.NotNull;

public record UpdateAccountTypeRequest(
    @NotNull(message = "Account type is required")
    AccountType type
) {}
