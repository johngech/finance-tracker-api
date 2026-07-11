package com.marakicode.financetracker.reports;

import com.marakicode.financetracker.auth.JwtService;
import com.marakicode.financetracker.reports.dto.AccountBreakdownResponse;
import com.marakicode.financetracker.reports.dto.CategoryBreakdownResponse;
import com.marakicode.financetracker.reports.dto.MonthlyBreakdownResponse;
import com.marakicode.financetracker.reports.dto.SummaryResponse;
import com.marakicode.financetracker.transactions.TransactionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReportsController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("Reports Controller Tests")
class ReportsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReportsService reportsService;

    @MockitoBean
    private JwtService jwtService;

    @Test
    @DisplayName("getSummary_shouldReturn200_withData - GET /reports/summary returns income/expense summary")
    void getSummary_shouldReturn200_withData() throws Exception {
        // Arrange
        var response = new SummaryResponse(
                new BigDecimal("5000.00"), new BigDecimal("3200.00"),
                new BigDecimal("1800.00"), 25L);
        when(reportsService.getSummary(any(), any())).thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/v1/reports/summary")
                        .param("from", "2025-07-01")
                        .param("to", "2025-07-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalIncome").value(5000.00))
                .andExpect(jsonPath("$.data.totalExpense").value(3200.00))
                .andExpect(jsonPath("$.data.netBalance").value(1800.00))
                .andExpect(jsonPath("$.data.transactionCount").value(25));
    }

    @Test
    @DisplayName("getSummary_shouldReturn200_noParams - GET /reports/summary with no params returns all data")
    void getSummary_shouldReturn200_noParams() throws Exception {
        // Arrange
        var response = new SummaryResponse(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0L);
        when(reportsService.getSummary(null, null)).thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/v1/reports/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.transactionCount").value(0));
    }

    @Test
    @DisplayName("getSummary_shouldReturn400_fromAfterTo - from date after to date returns 400")
    void getSummary_shouldReturn400_fromAfterTo() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/reports/summary")
                        .param("from", "2025-12-01")
                        .param("to", "2025-01-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("'from' date must not be after 'to' date"));
    }

    @Test
    @DisplayName("getCategoryBreakdown_shouldReturn200_withData - GET /reports/by-category returns category list")
    void getCategoryBreakdown_shouldReturn200_withData() throws Exception {
        // Arrange
        var response = List.of(
                new CategoryBreakdownResponse("FOOD", new BigDecimal("500.00"), new BigDecimal("45.45")),
                new CategoryBreakdownResponse("HOUSING", new BigDecimal("600.00"), new BigDecimal("54.55")));
        when(reportsService.getCategoryBreakdown(any(), any(), any())).thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/v1/reports/by-category"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].category").value("FOOD"))
                .andExpect(jsonPath("$.data[0].totalAmount").value(500.00))
                .andExpect(jsonPath("$.data[0].percentage").value(45.45))
                .andExpect(jsonPath("$.data[1].category").value("HOUSING"))
                .andExpect(jsonPath("$.data[1].totalAmount").value(600.00));
    }

    @Test
    @DisplayName("getCategoryBreakdown_shouldReturn200_withTypeFilter - GET with type=INCOME passes filter")
    void getCategoryBreakdown_shouldReturn200_withTypeFilter() throws Exception {
        // Arrange
        var response = List.of(
                new CategoryBreakdownResponse("SALARY", new BigDecimal("5000.00"), new BigDecimal("100.00")));
        when(reportsService.getCategoryBreakdown(eq(TransactionType.INCOME), any(), any()))
                .thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/v1/reports/by-category")
                        .param("type", "INCOME"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].category").value("SALARY"));
    }

    @Test
    @DisplayName("getCategoryBreakdown_shouldReturn400_fromAfterTo - invalid date range returns 400")
    void getCategoryBreakdown_shouldReturn400_fromAfterTo() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/reports/by-category")
                        .param("from", "2025-12-01")
                        .param("to", "2025-01-01"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("getMonthlyBreakdown_shouldReturn200_withData - GET /reports/monthly returns month list")
    void getMonthlyBreakdown_shouldReturn200_withData() throws Exception {
        // Arrange
        var response = List.of(
                new MonthlyBreakdownResponse(7, new BigDecimal("3000.00"), new BigDecimal("1500.00")),
                new MonthlyBreakdownResponse(8, new BigDecimal("3500.00"), new BigDecimal("1200.00")));
        when(reportsService.getMonthlyBreakdown(2025)).thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/v1/reports/monthly")
                        .param("year", "2025"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].month").value(7))
                .andExpect(jsonPath("$.data[0].income").value(3000.00))
                .andExpect(jsonPath("$.data[0].expense").value(1500.00))
                .andExpect(jsonPath("$.data[1].month").value(8));
    }

    @Test
    @DisplayName("getMonthlyBreakdown_shouldReturn400_missingYear - missing year param returns 400")
    void getMonthlyBreakdown_shouldReturn400_missingYear() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/reports/monthly"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("getAccountBreakdown_shouldReturn200_withData - GET /reports/by-account returns account list")
    void getAccountBreakdown_shouldReturn200_withData() throws Exception {
        // Arrange
        var response = List.of(
                new AccountBreakdownResponse(1L, "Checking", new BigDecimal("5000.00"),
                        new BigDecimal("3000.00"), new BigDecimal("2000.00")),
                new AccountBreakdownResponse(2L, "Savings", new BigDecimal("1000.00"),
                        BigDecimal.ZERO, new BigDecimal("1000.00")));
        when(reportsService.getAccountBreakdown(any(), any())).thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/v1/reports/by-account"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].accountId").value(1))
                .andExpect(jsonPath("$.data[0].accountName").value("Checking"))
                .andExpect(jsonPath("$.data[0].totalIncome").value(5000.00))
                .andExpect(jsonPath("$.data[0].totalExpense").value(3000.00))
                .andExpect(jsonPath("$.data[0].netAmount").value(2000.00))
                .andExpect(jsonPath("$.data[1].accountId").value(2))
                .andExpect(jsonPath("$.data[1].accountName").value("Savings"));
    }

    @Test
    @DisplayName("getAccountBreakdown_shouldReturn200_withDates - GET with from/to passes dates")
    void getAccountBreakdown_shouldReturn200_withDates() throws Exception {
        // Arrange
        var response = List.<AccountBreakdownResponse>of();
        when(reportsService.getAccountBreakdown(any(), any())).thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/v1/reports/by-account")
                        .param("from", "2025-07-01")
                        .param("to", "2025-07-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    @DisplayName("getAccountBreakdown_shouldReturn400_fromAfterTo - invalid date range returns 400")
    void getAccountBreakdown_shouldReturn400_fromAfterTo() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/reports/by-account")
                        .param("from", "2025-12-01")
                        .param("to", "2025-01-01"))
                .andExpect(status().isBadRequest());
    }
}
