package com.marakicode.financetracker.transactions.dto;

import com.marakicode.financetracker.transactions.TransactionType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "Transaction information")
public record TransactionResponse(
    @Schema(example = "1", description = "Unique transaction identifier")
    Long id,

    @Schema(example = "1", description = "ID of the associated account")
    Long accountId,

    @Schema(example = "MyChecking", description = "Name of the associated account")
    String accountName,

    @Schema(description = "Transaction type")
    TransactionType type,

    @Schema(example = "50.00", description = "Transaction amount")
    BigDecimal amount,

    @Schema(example = "Groceries at Supermarket", description = "Transaction description")
    String description,

    @Schema(example = "2026-07-12", description = "Transaction date")
    LocalDate transactionDate,

    @Schema(example = "Food & Dining", description = "Transaction category")
    String category,

    @Schema(description = "Transaction creation timestamp")
    LocalDateTime createdAt
) {}
