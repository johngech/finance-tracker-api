package com.marakicode.financetracker.reports;

import com.marakicode.financetracker.common.CurrentUserProvider;
import com.marakicode.financetracker.reports.dto.AccountBreakdownResponse;
import com.marakicode.financetracker.reports.dto.CategoryBreakdownResponse;
import com.marakicode.financetracker.reports.dto.MonthlyBreakdownResponse;
import com.marakicode.financetracker.reports.dto.SummaryResponse;
import com.marakicode.financetracker.transactions.TransactionType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportsService {

    private final ReportsRepository reportsRepository;
    private final CurrentUserProvider currentUserProvider;

    @Transactional(readOnly = true)
    public SummaryResponse getSummary(LocalDate from, LocalDate to) {
        var userId = currentUserProvider.getCurrentUserId();
        List<Object[]> results = reportsRepository.getSummaryByUserId(userId, from, to);
        return ReportsMapper.mapToSummary(results.isEmpty() ? new Object[]{BigDecimal.ZERO, BigDecimal.ZERO, 0L} : results.get(0));
    }

    @Transactional(readOnly = true)
    public List<CategoryBreakdownResponse> getCategoryBreakdown(
            TransactionType type, LocalDate from, LocalDate to) {
        var userId = currentUserProvider.getCurrentUserId();
        String typeName = type != null ? type.name() : null;
        List<Object[]> results = reportsRepository.getCategoryBreakdown(
                userId, typeName, from, to);
        return ReportsMapper.mapToCategoryBreakdown(results);
    }

    @Transactional(readOnly = true)
    public List<MonthlyBreakdownResponse> getMonthlyBreakdown(int year) {
        var userId = currentUserProvider.getCurrentUserId();
        List<Object[]> results = reportsRepository.getMonthlyBreakdown(userId, year);
        return ReportsMapper.mapToMonthlyBreakdown(results);
    }

    @Transactional(readOnly = true)
    public List<AccountBreakdownResponse> getAccountBreakdown(LocalDate from, LocalDate to) {
        var userId = currentUserProvider.getCurrentUserId();
        List<Object[]> results = reportsRepository.getAccountBreakdown(
                userId, from, to);
        return ReportsMapper.mapToAccountBreakdown(results);
    }
}
