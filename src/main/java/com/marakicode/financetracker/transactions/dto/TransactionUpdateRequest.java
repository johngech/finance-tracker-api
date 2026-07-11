package com.marakicode.financetracker.transactions.dto;

import com.marakicode.financetracker.transactions.TransactionType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Request payload for updating an existing transaction (partial update)")
public record TransactionUpdateRequest(
    @Schema(description = "New transaction type")
    TransactionType type,

    @Schema(example = "75.00", description = "New transaction amount (must be at least 0.01)")
    @DecimalMin(value = "0.01", message = "Amount must be at least 0.01")
    BigDecimal amount,

    @Schema(example = "Updated description", description = "New description (max 255 characters)")
    @Size(max = 255, message = "Description must not exceed 255 characters")
    String description,

    @Schema(example = "2026-07-15", description = "New transaction date (ISO format, cannot be in the future)")
    @PastOrPresent(message = "Transaction date cannot be in the future")
    LocalDate transactionDate,

    @Schema(example = "Transportation", description = "New category (max 50 characters)")
    @Size(max = 50, message = "Category must not exceed 50 characters")
    String category
) {}
