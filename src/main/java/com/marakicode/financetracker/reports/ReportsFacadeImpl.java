package com.marakicode.financetracker.reports;

import com.marakicode.financetracker.reports.dto.AccountBreakdownResponse;
import com.marakicode.financetracker.reports.dto.CategoryBreakdownResponse;
import com.marakicode.financetracker.reports.dto.MonthlyBreakdownResponse;
import com.marakicode.financetracker.reports.dto.SummaryResponse;
import com.marakicode.financetracker.transactions.TransactionType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportsFacadeImpl implements ReportsFacade {

    private final ReportsRepository reportsRepository;

    @Override
    @Transactional(readOnly = true)
    public SummaryResponse getSystemSummary(LocalDate from, LocalDate to) {
        Object[] result = reportsRepository.getSystemSummary(from, to);
        return ReportsMapper.mapToSummary(result);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryBreakdownResponse> getSystemCategoryBreakdown(
            TransactionType type, LocalDate from, LocalDate to) {
        String typeName = type != null ? type.name() : null;
        List<Object[]> results = reportsRepository.getSystemCategoryBreakdown(typeName, from, to);
        return ReportsMapper.mapToCategoryBreakdown(results);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MonthlyBreakdownResponse> getSystemMonthlyBreakdown(int year) {
        List<Object[]> results = reportsRepository.getSystemMonthlyBreakdown(year);
        return ReportsMapper.mapToMonthlyBreakdown(results);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccountBreakdownResponse> getSystemAccountBreakdown(LocalDate from, LocalDate to) {
        List<Object[]> results = reportsRepository.getSystemAccountBreakdown(from, to);
        return ReportsMapper.mapToAccountBreakdown(results);
    }
}
