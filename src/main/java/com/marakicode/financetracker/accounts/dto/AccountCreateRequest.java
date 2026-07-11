package com.marakicode.financetracker.accounts.dto;

import com.marakicode.financetracker.accounts.AccountType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

@Schema(description = "Request payload for creating a new account")
public record AccountCreateRequest(
    @Schema(example = "MyChecking", description = "Account name (3-20 alphanumeric characters)")
    @NotBlank(message = "Account name is required")
    @Size(min = 3, max = 20, message = "Account name must be between 3 and 20 characters")
    @Pattern(regexp = "^[a-zA-Z0-9]+$", message = "Account name must contain only letters and numbers")
    String name,

    @Schema(description = "Account type")
    @NotNull(message = "Account type is required")
    AccountType type,

    @Schema(example = "USD", description = "3-letter ISO currency code")
    @NotBlank(message = "Currency is required")
    @Size(min = 3, max = 3, message = "Currency must be a 3-letter ISO code (e.g., USD)")
    String currency,

    @Schema(example = "1000.00", description = "Initial account balance (must be non-negative)")
    @NotNull(message = "Initial balance is required")
    @DecimalMin(value = "0.00", message = "Initial balance must be non-negative")
    BigDecimal initialBalance
) {}
