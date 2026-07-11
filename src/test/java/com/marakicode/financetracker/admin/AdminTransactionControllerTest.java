package com.marakicode.financetracker.admin;

import com.marakicode.financetracker.auth.JwtService;
import com.marakicode.financetracker.common.PagedResponse;
import com.marakicode.financetracker.common.ResourceNotFoundException;
import com.marakicode.financetracker.transactions.TransactionType;
import com.marakicode.financetracker.transactions.dto.TransactionSummary;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminTransactionController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminTransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminTransactionService adminTransactionService;

    @MockitoBean
    private JwtService jwtService;

    private static TransactionSummary sampleTransactionSummary() {
        return new TransactionSummary(
                1L, 10L, "Checking", 1L, "John Doe",
                TransactionType.EXPENSE, new BigDecimal("50.00"),
                "Lunch", LocalDate.of(2025, 6, 15), "Food",
                LocalDateTime.of(2025, 6, 15, 12, 30));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("listTransactions_shouldReturn200_withPagedResponse")
    void listTransactions_shouldReturn200_withPagedResponse() throws Exception {
        var txns = List.of(sampleTransactionSummary());
        var paged = new PagedResponse<>(txns, 0, 10, 1, 1);
        when(adminTransactionService.listTransactions(any(), any()))
                .thenReturn(paged);

        mockMvc.perform(get("/api/v1/admin/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].type").value("EXPENSE"))
                .andExpect(jsonPath("$.data.content[0].amount").value(50.00))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.totalPages").value(1));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("getTransaction_shouldReturn200_withValidId")
    void getTransaction_shouldReturn200_withValidId() throws Exception {
        var txn = sampleTransactionSummary();
        when(adminTransactionService.getTransaction(1L)).thenReturn(txn);

        mockMvc.perform(get("/api/v1/admin/transactions/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.type").value("EXPENSE"))
                .andExpect(jsonPath("$.data.amount").value(50.00))
                .andExpect(jsonPath("$.data.description").value("Lunch"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("getTransaction_shouldReturn404_whenNotFound")
    void getTransaction_shouldReturn404_whenNotFound() throws Exception {
        when(adminTransactionService.getTransaction(999L))
                .thenThrow(new ResourceNotFoundException(
                        "Transaction not found with id: 999"));

        mockMvc.perform(get("/api/v1/admin/transactions/999"))
                .andExpect(status().isNotFound());
    }

}
