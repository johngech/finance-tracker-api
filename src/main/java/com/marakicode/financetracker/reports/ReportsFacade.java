package com.marakicode.financetracker.reports;

import com.marakicode.financetracker.reports.dto.AccountBreakdownResponse;
import com.marakicode.financetracker.reports.dto.CategoryBreakdownResponse;
import com.marakicode.financetracker.reports.dto.MonthlyBreakdownResponse;
import com.marakicode.financetracker.reports.dto.SummaryResponse;
import com.marakicode.financetracker.transactions.TransactionType;

import java.time.LocalDate;
import java.util.List;

public interface ReportsFacade {
    SummaryResponse getSystemSummary(LocalDate from, LocalDate to);
    List<CategoryBreakdownResponse> getSystemCategoryBreakdown(
            TransactionType type, LocalDate from, LocalDate to);
    List<MonthlyBreakdownResponse> getSystemMonthlyBreakdown(int year);
    List<AccountBreakdownResponse> getSystemAccountBreakdown(LocalDate from, LocalDate to);
}
