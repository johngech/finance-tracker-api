package com.marakicode.financetracker.transactions.dto;

import com.marakicode.financetracker.transactions.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Admin-facing transaction summary with full ownership context.
 */
public record TransactionSummary(
    Long id,
    Long accountId,
    String accountName,
    Long userId,
    String userName,
    TransactionType type,
    BigDecimal amount,
    String description,
    LocalDate transactionDate,
    String category,
    LocalDateTime createdAt
) {}
