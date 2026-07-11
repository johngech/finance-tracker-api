package com.marakicode.financetracker.reports.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(description = "Monthly income vs expense breakdown")
public record MonthlyBreakdownResponse(
    @Schema(example = "1", description = "Month number (1-12)")
    int month,

    @Schema(example = "2000.00", description = "Total income for the month")
    BigDecimal income,

    @Schema(example = "1500.00", description = "Total expense for the month")
    BigDecimal expense
) {}
