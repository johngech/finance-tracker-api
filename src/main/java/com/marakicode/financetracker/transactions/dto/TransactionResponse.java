package com.marakicode.financetracker.transactions.dto;

import com.marakicode.financetracker.transactions.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record TransactionResponse(
    Long id,
    Long accountId,
    String accountName,
    TransactionType type,
    BigDecimal amount,
    String description,
    LocalDate transactionDate,
    String category,
    LocalDateTime createdAt
) {}
