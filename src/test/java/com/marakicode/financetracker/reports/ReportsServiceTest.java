package com.marakicode.financetracker.reports;

import com.marakicode.financetracker.common.SecurityUtils;
import com.marakicode.financetracker.reports.dto.AccountBreakdownResponse;
import com.marakicode.financetracker.reports.dto.CategoryBreakdownResponse;
import com.marakicode.financetracker.reports.dto.MonthlyBreakdownResponse;
import com.marakicode.financetracker.reports.dto.SummaryResponse;
import com.marakicode.financetracker.transactions.TransactionType;
import com.marakicode.financetracker.users.Role;
import com.marakicode.financetracker.users.User;
import com.marakicode.financetracker.users.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Reports Service Tests")
class ReportsServiceTest {

    @Mock
    private ReportsRepository reportsRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private ReportsService reportsService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");
        user.setRole(Role.USER);

        var auth = org.mockito.Mockito.mock(org.springframework.security.core.Authentication.class);
        org.mockito.Mockito.lenient().when(auth.getName()).thenReturn("test@example.com");
        SecurityContextHolder.getContext().setAuthentication(auth);

        org.mockito.Mockito.lenient().when(userService.findByEmail("test@example.com")).thenReturn(user);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // --- getSummary tests ---

    @Test
    @DisplayName("getSummary returns correct response - maps Object[] to SummaryResponse")
    void getSummary_returnsCorrectResponse() {
        // Arrange
        Object[] mockResult = new Object[]{
                new BigDecimal("1500.00"),
                new BigDecimal("700.00"),
                10L
        };
        when(reportsRepository.getSummaryByUserId(eq(1L), any(), any())).thenReturn(mockResult);

        // Act
        var result = reportsService.getSummary(
                LocalDate.of(2025, 7, 1), LocalDate.of(2025, 7, 31));

        // Assert
        assertThat(result.totalIncome()).isEqualByComparingTo(new BigDecimal("1500.00"));
        assertThat(result.totalExpense()).isEqualByComparingTo(new BigDecimal("700.00"));
        assertThat(result.netBalance()).isEqualByComparingTo(new BigDecimal("800.00"));
        assertThat(result.transactionCount()).isEqualTo(10);
    }

    @Test
    @DisplayName("getSummary with empty result returns zeros - zero transactions returns zeroed response")
    void getSummary_emptyResult_returnsZeros() {
        // Arrange
        Object[] mockResult = new Object[]{
                BigDecimal.ZERO, BigDecimal.ZERO, 0L
        };
        when(reportsRepository.getSummaryByUserId(eq(1L), any(), any())).thenReturn(mockResult);

        // Act
        var result = reportsService.getSummary(null, null);

        // Assert
        assertThat(result.totalIncome()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.totalExpense()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.netBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.transactionCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("getSummary delegates to repository - passes userId and dates correctly")
    void getSummary_delegatesToRepository() {
        // Arrange
        Object[] mockResult = new Object[]{
                BigDecimal.ZERO, BigDecimal.ZERO, 0L
        };
        LocalDate from = LocalDate.of(2025, 7, 1);
        LocalDate to = LocalDate.of(2025, 7, 31);
        when(reportsRepository.getSummaryByUserId(1L, from, to)).thenReturn(mockResult);

        // Act
        reportsService.getSummary(from, to);

        // Assert
        verify(reportsRepository).getSummaryByUserId(1L, from, to);
    }

    // --- getCategoryBreakdown tests ---

    @Test
    @DisplayName("getCategoryBreakdown computes percentages - categoryAmount/totalAmount * 100")
    void getCategoryBreakdown_computesPercentages() {
        // Arrange
        List<Object[]> mockResults = List.of(
                new Object[]{"HOUSING", new BigDecimal("600.00")},
                new Object[]{"FOOD", new BigDecimal("300.00")},
                new Object[]{"TRANSPORT", new BigDecimal("100.00")}
        );
        when(reportsRepository.getCategoryBreakdown(eq(1L), any(), any(), any())).thenReturn(mockResults);

        // Act
        var result = reportsService.getCategoryBreakdown(null, null, null);

        // Assert
        assertThat(result).hasSize(3);
        assertThat(result.get(0).category()).isEqualTo("HOUSING");
        assertThat(result.get(0).percentage()).isEqualByComparingTo(new BigDecimal("60.00"));
        assertThat(result.get(1).category()).isEqualTo("FOOD");
        assertThat(result.get(1).percentage()).isEqualByComparingTo(new BigDecimal("30.00"));
        assertThat(result.get(2).category()).isEqualTo("TRANSPORT");
        assertThat(result.get(2).percentage()).isEqualByComparingTo(new BigDecimal("10.00"));
    }

    @Test
    @DisplayName("getCategoryBreakdown with empty result returns empty list - no transactions returns empty")
    void getCategoryBreakdown_emptyResult_returnsEmptyList() {
        // Arrange
        when(reportsRepository.getCategoryBreakdown(eq(1L), any(), any(), any()))
                .thenReturn(List.of());

        // Act
        var result = reportsService.getCategoryBreakdown(TransactionType.EXPENSE, null, null);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getCategoryBreakdown passes type name - converts enum to String for repository")
    void getCategoryBreakdown_passesTypeName() {
        // Arrange
        when(reportsRepository.getCategoryBreakdown(eq(1L), eq("INCOME"), any(), any()))
                .thenReturn(List.of());

        // Act
        reportsService.getCategoryBreakdown(TransactionType.INCOME, null, null);

        // Assert
        verify(reportsRepository).getCategoryBreakdown(1L, "INCOME", null, null);
    }

    // --- getMonthlyBreakdown tests ---

    @Test
    @DisplayName("getMonthlyBreakdown maps correctly - maps month/income/expense tuples")
    void getMonthlyBreakdown_mapsCorrectly() {
        // Arrange
        List<Object[]> mockResults = List.of(
                new Object[]{7, new BigDecimal("3000.00"), new BigDecimal("1500.00")},
                new Object[]{8, new BigDecimal("3500.00"), new BigDecimal("1200.00")}
        );
        when(reportsRepository.getMonthlyBreakdown(1L, 2025)).thenReturn(mockResults);

        // Act
        var result = reportsService.getMonthlyBreakdown(2025);

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result.get(0).month()).isEqualTo(7);
        assertThat(result.get(0).income()).isEqualByComparingTo(new BigDecimal("3000.00"));
        assertThat(result.get(0).expense()).isEqualByComparingTo(new BigDecimal("1500.00"));
        assertThat(result.get(1).month()).isEqualTo(8);
    }

    @Test
    @DisplayName("getMonthlyBreakdown for empty year returns empty list - no transactions for year")
    void getMonthlyBreakdown_emptyYear_returnsEmptyList() {
        // Arrange
        when(reportsRepository.getMonthlyBreakdown(1L, 2099)).thenReturn(List.of());

        // Act
        var result = reportsService.getMonthlyBreakdown(2099);

        // Assert
        assertThat(result).isEmpty();
    }

    // --- getAccountBreakdown tests ---

    @Test
    @DisplayName("getAccountBreakdown computes net - net equals income minus expense per account")
    void getAccountBreakdown_computesNet() {
        // Arrange
        List<Object[]> mockResults = List.of(
                new Object[]{1L, "Checking", new BigDecimal("5000.00"), new BigDecimal("3000.00")},
                new Object[]{2L, "Savings", new BigDecimal("1000.00"), new BigDecimal("0.00")}
        );
        when(reportsRepository.getAccountBreakdown(eq(1L), any(), any())).thenReturn(mockResults);

        // Act
        var result = reportsService.getAccountBreakdown(null, null);

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result.get(0).accountId()).isEqualTo(1L);
        assertThat(result.get(0).accountName()).isEqualTo("Checking");
        assertThat(result.get(0).netAmount()).isEqualByComparingTo(new BigDecimal("2000.00"));
        assertThat(result.get(1).accountId()).isEqualTo(2L);
        assertThat(result.get(1).netAmount()).isEqualByComparingTo(new BigDecimal("1000.00"));
    }

    @Test
    @DisplayName("getAccountBreakdown with empty result returns empty list - no accounts returns empty")
    void getAccountBreakdown_emptyResult_returnsEmptyList() {
        // Arrange
        when(reportsRepository.getAccountBreakdown(eq(1L), any(), any())).thenReturn(List.of());

        // Act
        var result = reportsService.getAccountBreakdown(null, null);

        // Assert
        assertThat(result).isEmpty();
    }
}
