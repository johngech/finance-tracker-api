package com.marakicode.financetracker.reports.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(description = "Income/expense summary")
public record SummaryResponse(
    @Schema(example = "5000.00", description = "Total income amount")
    BigDecimal totalIncome,

    @Schema(example = "3200.00", description = "Total expense amount")
    BigDecimal totalExpense,

    @Schema(example = "1800.00", description = "Net balance (income - expense)")
    BigDecimal netBalance,

    @Schema(example = "42", description = "Total number of transactions")
    long transactionCount
) {}
