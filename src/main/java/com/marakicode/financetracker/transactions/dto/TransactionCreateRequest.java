package com.marakicode.financetracker.transactions.dto;

import com.marakicode.financetracker.transactions.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionCreateRequest(
    @NotNull(message = "Account ID is required")
    Long accountId,

    @NotNull(message = "Transaction type is required")
    TransactionType type,

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be at least 0.01")
    BigDecimal amount,

    @Size(max = 255, message = "Description must not exceed 255 characters")
    String description,

    @NotNull(message = "Transaction date is required")
    @PastOrPresent(message = "Transaction date cannot be in the future")
    LocalDate transactionDate,

    @Size(max = 50, message = "Category must not exceed 50 characters")
    String category
) {}
