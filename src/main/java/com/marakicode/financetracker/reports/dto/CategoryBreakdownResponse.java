package com.marakicode.financetracker.reports.dto;

import java.math.BigDecimal;

public record CategoryBreakdownResponse(
    String category,
    BigDecimal totalAmount,
    BigDecimal percentage
) {}
