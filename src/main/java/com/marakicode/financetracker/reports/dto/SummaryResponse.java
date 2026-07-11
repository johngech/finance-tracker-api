package com.marakicode.financetracker.reports.dto;

import java.math.BigDecimal;

public record SummaryResponse(
    BigDecimal totalIncome,
    BigDecimal totalExpense,
    BigDecimal netBalance,
    long transactionCount
) {}
