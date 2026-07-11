package com.marakicode.financetracker.reports.dto;

import java.math.BigDecimal;

public record AccountBreakdownResponse(
    Long accountId,
    String accountName,
    BigDecimal totalIncome,
    BigDecimal totalExpense,
    BigDecimal netAmount
) {}
