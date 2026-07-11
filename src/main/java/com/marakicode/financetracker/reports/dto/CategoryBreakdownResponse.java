package com.marakicode.financetracker.reports.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(description = "Spending breakdown by category")
public record CategoryBreakdownResponse(
    @Schema(example = "Food & Dining", description = "Transaction category name")
    String category,

    @Schema(example = "1200.00", description = "Total amount spent in this category")
    BigDecimal totalAmount,

    @Schema(example = "37.50", description = "Percentage of total spending")
    BigDecimal percentage
) {}
