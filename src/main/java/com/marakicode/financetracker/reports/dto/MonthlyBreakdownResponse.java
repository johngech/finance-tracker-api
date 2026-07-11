package com.marakicode.financetracker.reports.dto;

import java.math.BigDecimal;

public record MonthlyBreakdownResponse(
    int month,
    BigDecimal income,
    BigDecimal expense
) {}
