package com.marakicode.financetracker.transactions.dto;

import java.math.BigDecimal;

/**
 * System-wide transaction statistics for the admin dashboard.
 */
public record TransactionStatistics(
    long totalTransactions,
    long incomeCount,
    long expenseCount,
    BigDecimal totalIncome,
    BigDecimal totalExpense,
    BigDecimal netAmount
) {}
