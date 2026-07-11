package com.marakicode.financetracker.admin;

import com.marakicode.financetracker.accounts.AccountsFacade;
import com.marakicode.financetracker.accounts.dto.AccountStatistics;
import com.marakicode.financetracker.admin.dto.DashboardResponse;
import com.marakicode.financetracker.common.ResourceNotFoundException;
import com.marakicode.financetracker.transactions.TransactionsFacade;
import com.marakicode.financetracker.transactions.dto.TransactionStatistics;
import com.marakicode.financetracker.users.dto.UserStatistics;
import com.marakicode.financetracker.users.UsersFacade;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminDashboardServiceTest {

    @Mock
    private UsersFacade usersFacade;

    @Mock
    private AccountsFacade accountsFacade;

    @Mock
    private TransactionsFacade transactionsFacade;

    @InjectMocks
    private AdminDashboardService adminDashboardService;

    @Test
    @DisplayName("getDashboard_aggregatesAllFacades")
    void getDashboard_aggregatesAllFacades() {
        var userStats = new UserStatistics(100, 90, 10, 5, 95);
        var accountStats =                 new AccountStatistics(
                50, 3, new BigDecimal("250000"), Map.of("CHECKING", 30L));
        var txStats = new TransactionStatistics(
                500, 200, 300,
                new BigDecimal("50000"),
                new BigDecimal("30000"),
                new BigDecimal("20000"));

        when(usersFacade.getStatistics()).thenReturn(userStats);
        when(accountsFacade.getStatistics()).thenReturn(accountStats);
        when(transactionsFacade.getStatistics()).thenReturn(txStats);

        DashboardResponse result = adminDashboardService.getDashboard();

        assertThat(result.userStats()).isEqualTo(userStats);
        assertThat(result.accountStats()).isEqualTo(accountStats);
        assertThat(result.transactionStats()).isEqualTo(txStats);
        verify(usersFacade).getStatistics();
        verify(accountsFacade).getStatistics();
        verify(transactionsFacade).getStatistics();
    }

    @Test
    @DisplayName("getDashboard_propagatesException")
    void getDashboard_propagatesException() {
        when(usersFacade.getStatistics())
                .thenThrow(new ResourceNotFoundException("Service down"));

        assertThatThrownBy(() -> adminDashboardService.getDashboard())
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
