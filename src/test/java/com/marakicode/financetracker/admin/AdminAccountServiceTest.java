package com.marakicode.financetracker.admin;

import com.marakicode.financetracker.accounts.AccountsFacade;
import com.marakicode.financetracker.accounts.dto.AccountStatistics;
import com.marakicode.financetracker.accounts.dto.AccountSummary;
import com.marakicode.financetracker.accounts.AccountType;
import com.marakicode.financetracker.common.PagedResponse;
import com.marakicode.financetracker.common.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminAccountServiceTest {

    @Mock
    private AccountsFacade accountsFacade;

    @InjectMocks
    private AdminAccountService adminAccountService;

    private static AccountSummary sampleAccountSummary() {
        return new AccountSummary(
                1L, 10L, "John Doe", "Checking",
                AccountType.CHECKING, new BigDecimal("5000.00"),
                "USD", false, LocalDateTime.now());
    }

    private static AccountStatistics sampleStats() {
        return new AccountStatistics(
                50, 3, new BigDecimal("250000.00"),
                java.util.Map.of("CHECKING", 30L, "SAVINGS", 20L));
    }

    @BeforeEach
    void setUp() {
        lenient().when(accountsFacade.getStatistics()).thenReturn(sampleStats());
    }

    @Test
    @DisplayName("listAccounts_delegatesToFacade")
    void listAccounts_delegatesToFacade() {
        var pageable = PageRequest.of(0, 10);
        var expected = new PagedResponse<>(
                List.of(sampleAccountSummary()), 0, 10, 1, 1);
        when(accountsFacade.listAccounts("checking", pageable))
                .thenReturn(expected);

        var result = adminAccountService.listAccounts("checking", pageable);

        assertThat(result).isEqualTo(expected);
        verify(accountsFacade).listAccounts("checking", pageable);
    }

    @Test
    @DisplayName("getAccount_delegatesToFacade")
    void getAccount_delegatesToFacade() {
        var expected = sampleAccountSummary();
        when(accountsFacade.getAccountById(1L)).thenReturn(expected);

        var result = adminAccountService.getAccount(1L);

        assertThat(result).isEqualTo(expected);
        verify(accountsFacade).getAccountById(1L);
    }

    @Test
    @DisplayName("freezeAccount_delegatesToFacade")
    void freezeAccount_delegatesToFacade() {
        adminAccountService.freezeAccount(1L);

        verify(accountsFacade).freezeAccount(1L);
    }

    @Test
    @DisplayName("unfreezeAccount_delegatesToFacade")
    void unfreezeAccount_delegatesToFacade() {
        adminAccountService.unfreezeAccount(1L);

        verify(accountsFacade).unfreezeAccount(1L);
    }

    @Test
    @DisplayName("getStatistics_delegatesToFacade")
    void getStatistics_delegatesToFacade() {
        var result = adminAccountService.getStatistics();

        assertThat(result).isEqualTo(sampleStats());
        verify(accountsFacade).getStatistics();
    }

    @Test
    @DisplayName("getAccount_propagatesResourceNotFoundException")
    void getAccount_propagatesResourceNotFoundException() {
        when(accountsFacade.getAccountById(999L))
            .thenThrow(new ResourceNotFoundException("Account not found"));
        assertThatThrownBy(() -> adminAccountService.getAccount(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Account not found");
    }

    @Test
    @DisplayName("freezeAccount_propagatesResourceNotFoundException")
    void freezeAccount_propagatesResourceNotFoundException() {
        doThrow(new ResourceNotFoundException("Account not found"))
            .when(accountsFacade).freezeAccount(999L);
        assertThatThrownBy(() -> adminAccountService.freezeAccount(999L))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("unfreezeAccount_propagatesResourceNotFoundException")
    void unfreezeAccount_propagatesResourceNotFoundException() {
        doThrow(new ResourceNotFoundException("Account not found"))
            .when(accountsFacade).unfreezeAccount(999L);
        assertThatThrownBy(() -> adminAccountService.unfreezeAccount(999L))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("listAccounts_propagatesException")
    void listAccounts_propagatesException() {
        var pageable = PageRequest.of(0, 10);
        when(accountsFacade.listAccounts(any(), any()))
            .thenThrow(new RuntimeException("DB error"));
        assertThatThrownBy(() -> adminAccountService.listAccounts(null, pageable))
            .isInstanceOf(RuntimeException.class);
    }
}
