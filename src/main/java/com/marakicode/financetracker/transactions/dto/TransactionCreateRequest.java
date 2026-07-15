package com.marakicode.financetracker.transactions.dto;

import com.marakicode.financetracker.transactions.TransactionType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

@Schema(description = "Request payload for creating a new transaction")
public record TransactionCreateRequest(
    @Schema(example = "1", description = "ID of the account to associate this transaction with")
    @NotNull(message = "Account ID is required")
    Long accountId,

    @Schema(description = "Transaction type (INCOME or EXPENSE)")
    @NotNull(message = "Transaction type is required")
    TransactionType type,

    @Schema(example = "50.00", description = "Transaction amount (must be at least 0.01)")
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be at least 0.01")
    BigDecimal amount,

    @Schema(example = "Groceries at Supermarket", description = "Optional transaction description (max 255 characters)")
    @Size(max = 255, message = "Description must not exceed 255 characters")
    String description,

    @Schema(example = "Food & Dining", description = "Optional transaction category (max 50 characters)")
    @Size(max = 50, message = "Category must not exceed 50 characters")
    String category
) {}
