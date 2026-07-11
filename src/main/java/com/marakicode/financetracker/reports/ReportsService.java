package com.marakicode.financetracker.reports;

import com.marakicode.financetracker.common.SecurityUtils;
import com.marakicode.financetracker.reports.dto.AccountBreakdownResponse;
import com.marakicode.financetracker.reports.dto.CategoryBreakdownResponse;
import com.marakicode.financetracker.reports.dto.MonthlyBreakdownResponse;
import com.marakicode.financetracker.reports.dto.SummaryResponse;
import com.marakicode.financetracker.transactions.TransactionType;
import com.marakicode.financetracker.users.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportsService {

    private final ReportsRepository reportsRepository;
    private final UserService userService;

    @Transactional(readOnly = true)
    public SummaryResponse getSummary(LocalDate from, LocalDate to) {
        var user = SecurityUtils.getCurrentUser(userService);
        Object[] result = reportsRepository.getSummaryByUserId(user.getId(), from, to);
        return mapToSummary(result);
    }

    @Transactional(readOnly = true)
    public List<CategoryBreakdownResponse> getCategoryBreakdown(
            TransactionType type, LocalDate from, LocalDate to) {
        var user = SecurityUtils.getCurrentUser(userService);
        String typeName = type != null ? type.name() : null;
        List<Object[]> results = reportsRepository.getCategoryBreakdown(
                user.getId(), typeName, from, to);
        return mapToCategoryBreakdown(results);
    }

    @Transactional(readOnly = true)
    public List<MonthlyBreakdownResponse> getMonthlyBreakdown(int year) {
        var user = SecurityUtils.getCurrentUser(userService);
        List<Object[]> results = reportsRepository.getMonthlyBreakdown(user.getId(), year);
        return mapToMonthlyBreakdown(results);
    }

    @Transactional(readOnly = true)
    public List<AccountBreakdownResponse> getAccountBreakdown(LocalDate from, LocalDate to) {
        var user = SecurityUtils.getCurrentUser(userService);
        List<Object[]> results = reportsRepository.getAccountBreakdown(
                user.getId(), from, to);
        return mapToAccountBreakdown(results);
    }

    private SummaryResponse mapToSummary(Object[] row) {
        BigDecimal totalIncome = (BigDecimal) row[0];
        BigDecimal totalExpense = (BigDecimal) row[1];
        long transactionCount = ((Number) row[2]).longValue();
        BigDecimal netBalance = totalIncome.subtract(totalExpense);
        return new SummaryResponse(totalIncome, totalExpense, netBalance, transactionCount);
    }

    private List<CategoryBreakdownResponse> mapToCategoryBreakdown(List<Object[]> results) {
        BigDecimal grandTotal = sumCategoryAmounts(results);
        List<CategoryBreakdownResponse> response = new ArrayList<>();
        for (Object[] row : results) {
            String category = (String) row[0];
            BigDecimal totalAmount = (BigDecimal) row[1];
            BigDecimal percentage = computePercentage(totalAmount, grandTotal);
            response.add(new CategoryBreakdownResponse(category, totalAmount, percentage));
        }
        return response;
    }

    private List<MonthlyBreakdownResponse> mapToMonthlyBreakdown(List<Object[]> results) {
        List<MonthlyBreakdownResponse> response = new ArrayList<>();
        for (Object[] row : results) {
            int month = ((Number) row[0]).intValue();
            BigDecimal income = (BigDecimal) row[1];
            BigDecimal expense = (BigDecimal) row[2];
            response.add(new MonthlyBreakdownResponse(month, income, expense));
        }
        return response;
    }

    private List<AccountBreakdownResponse> mapToAccountBreakdown(List<Object[]> results) {
        List<AccountBreakdownResponse> response = new ArrayList<>();
        for (Object[] row : results) {
            Long accountId = ((Number) row[0]).longValue();
            String accountName = (String) row[1];
            BigDecimal income = (BigDecimal) row[2];
            BigDecimal expense = (BigDecimal) row[3];
            BigDecimal net = income.subtract(expense);
            response.add(new AccountBreakdownResponse(
                    accountId, accountName, income, expense, net));
        }
        return response;
    }

    private BigDecimal sumCategoryAmounts(List<Object[]> results) {
        return results.stream()
                .map(row -> (BigDecimal) row[1])
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal computePercentage(BigDecimal amount, BigDecimal total) {
        if (total.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        return amount.multiply(BigDecimal.valueOf(100))
                .divide(total, 2, RoundingMode.HALF_UP);
    }
}
