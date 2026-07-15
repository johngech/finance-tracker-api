package com.marakicode.financetracker.reports;

import com.marakicode.financetracker.reports.dto.CategoryBreakdownResponse;
import com.marakicode.financetracker.reports.dto.MonthlyBreakdownResponse;
import com.marakicode.financetracker.reports.dto.SummaryResponse;
import com.marakicode.financetracker.reports.dto.AccountBreakdownResponse;
import com.marakicode.financetracker.transactions.TransactionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Reports Facade Impl Tests")
class ReportsFacadeImplTest {

    @Mock
    private ReportsRepository reportsRepository;

    @InjectMocks
    private ReportsFacadeImpl reportsFacade;

    // --- getSystemSummary tests ---

    @Test
    @DisplayName("getSystemSummary with data returns correct totals")
    void getSystemSummary_withData_returnsCorrectTotals() {
        // Arrange
        List<Object[]> mockResult = List.<Object[]>of(new Object[]{
                new BigDecimal("5000.00"),
                new BigDecimal("3200.00"),
                25L
        });
        LocalDate from = LocalDate.of(2025, 1, 1);
        LocalDate to = LocalDate.of(2025, 12, 31);
        when(reportsRepository.getSystemSummary(from, to)).thenReturn(mockResult);

        // Act
        SummaryResponse result = reportsFacade.getSystemSummary(from, to);

        // Assert
        assertThat(result.totalIncome()).isEqualByComparingTo(new BigDecimal("5000.00"));
        assertThat(result.totalExpense()).isEqualByComparingTo(new BigDecimal("3200.00"));
        assertThat(result.netBalance()).isEqualByComparingTo(new BigDecimal("1800.00"));
        assertThat(result.transactionCount()).isEqualTo(25);
        verify(reportsRepository).getSystemSummary(from, to);
    }

    @Test
    @DisplayName("getSystemSummary with empty result returns zeros")
    void getSystemSummary_emptyResult_returnsZeros() {
        // Arrange
        List<Object[]> mockResult = List.<Object[]>of(new Object[]{
                BigDecimal.ZERO, BigDecimal.ZERO, 0L
        });
        when(reportsRepository.getSystemSummary(null, null)).thenReturn(mockResult);

        // Act
        SummaryResponse result = reportsFacade.getSystemSummary(null, null);

        // Assert
        assertThat(result.totalIncome()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.totalExpense()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.netBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.transactionCount()).isEqualTo(0);
    }

    // --- getSystemCategoryBreakdown tests ---

    @Test
    @DisplayName("getSystemCategoryBreakdown with data returns category amounts and percentages")
    void getSystemCategoryBreakdown_withData_returnsCategoryAmountsAndPercentages() {
        // Arrange
        List<Object[]> mockResults = List.of(
                new Object[]{"HOUSING", new BigDecimal("1200.00")},
                new Object[]{"FOOD", new BigDecimal("600.00")},
                new Object[]{"TRANSPORT", new BigDecimal("200.00")}
        );
        when(reportsRepository.getSystemCategoryBreakdown(eq("EXPENSE"), any(), any()))
                .thenReturn(mockResults);

        // Act
        var result = reportsFacade.getSystemCategoryBreakdown(
                TransactionType.EXPENSE, null, null);

        // Assert
        assertThat(result).hasSize(3);
        assertThat(result.get(0).category()).isEqualTo("HOUSING");
        assertThat(result.get(0).totalAmount()).isEqualByComparingTo(new BigDecimal("1200.00"));
        assertThat(result.get(0).percentage()).isEqualByComparingTo(new BigDecimal("60.00"));
        assertThat(result.get(1).category()).isEqualTo("FOOD");
        assertThat(result.get(1).percentage()).isEqualByComparingTo(new BigDecimal("30.00"));
        assertThat(result.get(2).category()).isEqualTo("TRANSPORT");
        assertThat(result.get(2).percentage()).isEqualByComparingTo(new BigDecimal("10.00"));
    }

    @Test
    @DisplayName("getSystemCategoryBreakdown with null type returns all categories")
    void getSystemCategoryBreakdown_nullType_returnsAllCategories() {
        // Arrange
        List<Object[]> mockResults = List.of(
                new Object[]{"HOUSING", new BigDecimal("500.00")},
                new Object[]{"SALARY", new BigDecimal("3000.00")}
        );
        when(reportsRepository.getSystemCategoryBreakdown(eq(null), any(), any()))
                .thenReturn(mockResults);

        // Act
        var result = reportsFacade.getSystemCategoryBreakdown(null, null, null);

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result.get(0).category()).isEqualTo("HOUSING");
        verify(reportsRepository).getSystemCategoryBreakdown(null, null, null);
    }

    // --- getSystemMonthlyBreakdown tests ---

    @Test
    @DisplayName("getSystemMonthlyBreakdown with data returns monthly data")
    void getSystemMonthlyBreakdown_withData_returnsMonthlyData() {
        // Arrange
        List<Object[]> mockResults = List.of(
                new Object[]{1, new BigDecimal("4000.00"), new BigDecimal("2500.00")},
                new Object[]{2, new BigDecimal("4200.00"), new BigDecimal("2100.00")}
        );
        when(reportsRepository.getSystemMonthlyBreakdown(2025)).thenReturn(mockResults);

        // Act
        var result = reportsFacade.getSystemMonthlyBreakdown(2025);

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result.get(0).month()).isEqualTo(1);
        assertThat(result.get(0).income()).isEqualByComparingTo(new BigDecimal("4000.00"));
        assertThat(result.get(0).expense()).isEqualByComparingTo(new BigDecimal("2500.00"));
        assertThat(result.get(1).month()).isEqualTo(2);
        assertThat(result.get(1).income()).isEqualByComparingTo(new BigDecimal("4200.00"));
        assertThat(result.get(1).expense()).isEqualByComparingTo(new BigDecimal("2100.00"));
        verify(reportsRepository).getSystemMonthlyBreakdown(2025);
    }

    // --- getSystemAccountBreakdown tests ---

    @Test
    @DisplayName("getSystemAccountBreakdown with data returns per-account data")
    void getSystemAccountBreakdown_withData_returnsPerAccountData() {
        // Arrange
        List<Object[]> mockResults = List.of(
                new Object[]{1L, "Checking", new BigDecimal("8000.00"), new BigDecimal("5000.00")},
                new Object[]{2L, "Savings", new BigDecimal("2000.00"), new BigDecimal("500.00")}
        );
        LocalDate from = LocalDate.of(2025, 1, 1);
        LocalDate to = LocalDate.of(2025, 6, 30);
        when(reportsRepository.getSystemAccountBreakdown(from, to)).thenReturn(mockResults);

        // Act
        var result = reportsFacade.getSystemAccountBreakdown(from, to);

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result.get(0).accountId()).isEqualTo(1L);
        assertThat(result.get(0).accountName()).isEqualTo("Checking");
        assertThat(result.get(0).totalIncome()).isEqualByComparingTo(new BigDecimal("8000.00"));
        assertThat(result.get(0).totalExpense()).isEqualByComparingTo(new BigDecimal("5000.00"));
        assertThat(result.get(0).netAmount()).isEqualByComparingTo(new BigDecimal("3000.00"));
        assertThat(result.get(1).accountId()).isEqualTo(2L);
        assertThat(result.get(1).accountName()).isEqualTo("Savings");
        assertThat(result.get(1).netAmount()).isEqualByComparingTo(new BigDecimal("1500.00"));
        verify(reportsRepository).getSystemAccountBreakdown(from, to);
    }

    @Test
    @DisplayName("getSystemMonthlyBreakdown_emptyResult_returnsEmptyList")
    void getSystemMonthlyBreakdown_emptyResult_returnsEmptyList() {
        when(reportsRepository.getSystemMonthlyBreakdown(2099))
            .thenReturn(Collections.emptyList());

        var result = reportsFacade.getSystemMonthlyBreakdown(2099);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getSystemAccountBreakdown_emptyResult_returnsEmptyList")
    void getSystemAccountBreakdown_emptyResult_returnsEmptyList() {
        LocalDate from = LocalDate.of(2025, 1, 1);
        LocalDate to = LocalDate.of(2025, 12, 31);
        when(reportsRepository.getSystemAccountBreakdown(from, to))
            .thenReturn(Collections.emptyList());

        var result = reportsFacade.getSystemAccountBreakdown(from, to);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getSystemCategoryBreakdown_emptyResult_returnsEmptyList")
    void getSystemCategoryBreakdown_emptyResult_returnsEmptyList() {
        LocalDate from = LocalDate.of(2025, 1, 1);
        LocalDate to = LocalDate.of(2025, 12, 31);
        when(reportsRepository.getSystemCategoryBreakdown("EXPENSE", from, to))
            .thenReturn(Collections.emptyList());

        var result = reportsFacade.getSystemCategoryBreakdown(
            TransactionType.EXPENSE, from, to);

        assertThat(result).isEmpty();
    }
}
