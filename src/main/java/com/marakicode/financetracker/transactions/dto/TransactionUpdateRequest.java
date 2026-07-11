package com.marakicode.financetracker.transactions.dto;

import com.marakicode.financetracker.transactions.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionUpdateRequest(
    TransactionType type,
    @DecimalMin(value = "0.01", message = "Amount must be at least 0.01")
    BigDecimal amount,
    @Size(max = 255, message = "Description must not exceed 255 characters")
    String description,
    @PastOrPresent(message = "Transaction date cannot be in the future")
    LocalDate transactionDate,
    @Size(max = 50, message = "Category must not exceed 50 characters")
    String category
) {}
