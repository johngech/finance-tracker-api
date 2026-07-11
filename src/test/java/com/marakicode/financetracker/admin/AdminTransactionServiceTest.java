package com.marakicode.financetracker.admin;

import com.marakicode.financetracker.common.PagedResponse;
import com.marakicode.financetracker.common.ResourceNotFoundException;
import com.marakicode.financetracker.transactions.TransactionsFacade;
import com.marakicode.financetracker.transactions.TransactionType;
import com.marakicode.financetracker.transactions.dto.TransactionStatistics;
import com.marakicode.financetracker.transactions.dto.TransactionSummary;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminTransactionServiceTest {

    @Mock
    private TransactionsFacade transactionsFacade;

    @InjectMocks
    private AdminTransactionService adminTransactionService;

    private static TransactionSummary sampleTransactionSummary() {
        return new TransactionSummary(
                1L, 10L, "Checking", 1L, "John Doe",
                TransactionType.EXPENSE, new BigDecimal("50.00"),
                "Lunch", LocalDate.now(), "Food",
                LocalDateTime.now());
    }

    private static TransactionStatistics sampleStats() {
        return new TransactionStatistics(
                500, 200, 300,
                new BigDecimal("50000.00"),
                new BigDecimal("30000.00"),
                new BigDecimal("20000.00"));
    }

    @Test
    @DisplayName("listTransactions_delegatesToFacade")
    void listTransactions_delegatesToFacade() {
        var pageable = PageRequest.of(0, 10);
        var expected = new PagedResponse<>(
                List.of(sampleTransactionSummary()), 0, 10, 1, 1);
        when(transactionsFacade.listTransactions("lunch", pageable))
                .thenReturn(expected);

        var result = adminTransactionService.listTransactions(
                "lunch", pageable);

        assertThat(result).isEqualTo(expected);
        verify(transactionsFacade).listTransactions("lunch", pageable);
    }

    @Test
    @DisplayName("getTransaction_delegatesToFacade")
    void getTransaction_delegatesToFacade() {
        var expected = sampleTransactionSummary();
        when(transactionsFacade.getTransactionById(1L)).thenReturn(expected);

        var result = adminTransactionService.getTransaction(1L);

        assertThat(result).isEqualTo(expected);
        verify(transactionsFacade).getTransactionById(1L);
    }

    @Test
    @DisplayName("getStatistics_delegatesToFacade")
    void getStatistics_delegatesToFacade() {
        when(transactionsFacade.getStatistics()).thenReturn(sampleStats());

        var result = adminTransactionService.getStatistics();

        assertThat(result).isEqualTo(sampleStats());
        verify(transactionsFacade).getStatistics();
    }

    @Test
    @DisplayName("getTransaction_propagatesResourceNotFoundException")
    void getTransaction_propagatesResourceNotFoundException() {
        when(transactionsFacade.getTransactionById(999L))
            .thenThrow(new ResourceNotFoundException("Transaction not found"));
        assertThatThrownBy(() -> adminTransactionService.getTransaction(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Transaction not found");
    }

    @Test
    @DisplayName("listTransactions_propagatesException")
    void listTransactions_propagatesException() {
        var pageable = PageRequest.of(0, 10);
        when(transactionsFacade.listTransactions(any(), any()))
            .thenThrow(new RuntimeException("DB error"));
        assertThatThrownBy(() -> adminTransactionService.listTransactions(null, pageable))
            .isInstanceOf(RuntimeException.class);
    }
}
