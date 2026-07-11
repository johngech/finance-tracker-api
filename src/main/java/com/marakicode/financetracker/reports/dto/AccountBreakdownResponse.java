package com.marakicode.financetracker.reports.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(description = "Per-account income/expense breakdown")
public record AccountBreakdownResponse(
    @Schema(example = "1", description = "Account identifier")
    Long accountId,

    @Schema(example = "MyChecking", description = "Account name")
    String accountName,

    @Schema(example = "3000.00", description = "Total income for this account")
    BigDecimal totalIncome,

    @Schema(example = "1800.00", description = "Total expense for this account")
    BigDecimal totalExpense,

    @Schema(example = "1200.00", description = "Net amount (income - expense)")
    BigDecimal netAmount
) {}
