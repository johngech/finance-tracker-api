package com.marakicode.financetracker.accounts.dto;

import com.marakicode.financetracker.accounts.AccountType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "Account information")
public record AccountResponse(
    @Schema(example = "1", description = "Unique account identifier")
    Long id,

    @Schema(example = "MyChecking", description = "Account name")
    String name,

    @Schema(description = "Account type")
    AccountType type,

    @Schema(example = "1500.00", description = "Current account balance")
    BigDecimal balance,

    @Schema(example = "USD", description = "Account currency (ISO 4217)")
    String currency,

    @Schema(description = "Account creation timestamp")
    LocalDateTime createdAt
) {}
