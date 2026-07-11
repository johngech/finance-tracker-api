package com.marakicode.financetracker.admin;

import com.marakicode.financetracker.accounts.dto.AccountStatistics;
import com.marakicode.financetracker.admin.dto.DashboardResponse;
import com.marakicode.financetracker.auth.JwtService;
import com.marakicode.financetracker.transactions.dto.TransactionStatistics;
import com.marakicode.financetracker.users.dto.UserStatistics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminDashboardController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminDashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminDashboardService adminDashboardService;

    @MockitoBean
    private JwtService jwtService;

    private static DashboardResponse sampleDashboardResponse() {
        return new DashboardResponse(
                new UserStatistics(100, 90, 10, 5, 95),
                new AccountStatistics(
                        50, 3, new BigDecimal("250000.00"),
                        Map.of("CHECKING", 30L, "SAVINGS", 20L)),
                new TransactionStatistics(
                        500, 200, 300,
                        new BigDecimal("50000.00"),
                        new BigDecimal("30000.00"),
                        new BigDecimal("20000.00")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("getDashboard_shouldReturn200_withStatistics")
    void getDashboard_shouldReturn200_withStatistics() throws Exception {
        var dashboard = sampleDashboardResponse();
        when(adminDashboardService.getDashboard()).thenReturn(dashboard);

        mockMvc.perform(get("/api/v1/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userStats.totalUsers").value(100))
                .andExpect(jsonPath("$.data.userStats.activeUsers").value(90))
                .andExpect(jsonPath("$.data.accountStats.totalAccounts").value(50))
                .andExpect(jsonPath("$.data.accountStats.frozenAccounts").value(3))
                .andExpect(jsonPath("$.data.transactionStats.totalTransactions").value(500))
                .andExpect(jsonPath("$.data.transactionStats.netAmount").value(20000.00));
    }

}
