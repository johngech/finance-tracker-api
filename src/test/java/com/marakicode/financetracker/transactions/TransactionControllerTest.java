package com.marakicode.financetracker.transactions;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marakicode.financetracker.auth.JwtService;
import com.marakicode.financetracker.common.PagedResponse;
import com.marakicode.financetracker.common.ResourceNotFoundException;
import com.marakicode.financetracker.transactions.dto.TransactionCreateRequest;
import com.marakicode.financetracker.transactions.dto.TransactionResponse;
import com.marakicode.financetracker.transactions.dto.TransactionUpdateRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransactionController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("Transaction Controller Tests")
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TransactionService transactionService;

    @MockitoBean
    private JwtService jwtService;

    private TransactionResponse createTestResponse() {
        return new TransactionResponse(1L, 1L, "Test Account", TransactionType.INCOME,
                new BigDecimal("100.00"), "Test description", LocalDate.of(2026, 1, 15),
                "Salary", LocalDateTime.of(2026, 1, 15, 10, 0));
    }

    @Test
    @DisplayName("createTransaction_shouldReturn201_withData - POST with valid body returns 201 with transaction data")
    void createTransaction_shouldReturn201_withData() throws Exception {
        // Arrange
        var request = new TransactionCreateRequest(1L, TransactionType.INCOME,
                new BigDecimal("100.00"), "Test description", LocalDate.of(2026, 1, 15), "Salary");
        var response = createTestResponse();
        when(transactionService.createTransaction(any(TransactionCreateRequest.class))).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/v1/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Transaction created successfully"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.accountId").value(1))
                .andExpect(jsonPath("$.data.accountName").value("Test Account"))
                .andExpect(jsonPath("$.data.type").value("INCOME"))
                .andExpect(jsonPath("$.data.amount").value(100.00))
                .andExpect(jsonPath("$.data.description").value("Test description"))
                .andExpect(jsonPath("$.data.category").value("Salary"));
    }

    @Test
    @DisplayName("createTransaction_shouldReturn400_withInvalidBody - POST with missing required fields returns 400")
    void createTransaction_shouldReturn400_withInvalidBody() throws Exception {
        // Arrange
        String body = """
                {
                    "description": "Missing fields"
                }
                """;

        // Act & Assert
        mockMvc.perform(post("/api/v1/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.fieldErrors").isArray())
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'accountId')]").exists())
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'type')]").exists())
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'amount')]").exists())
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'transactionDate')]").exists());
    }

    @Test
    @DisplayName("createTransaction_shouldReturn400_withInsufficientFunds - POST when funds insufficient returns 400")
    void createTransaction_shouldReturn400_withInsufficientFunds() throws Exception {
        // Arrange
        var request = new TransactionCreateRequest(1L, TransactionType.EXPENSE,
                new BigDecimal("99999.00"), "BIG expense", LocalDate.of(2026, 1, 15), null);
        when(transactionService.createTransaction(any(TransactionCreateRequest.class)))
                .thenThrow(new InsufficientFundsException("Insufficient funds. Current balance: 1000.00"));

        // Act & Assert
        mockMvc.perform(post("/api/v1/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value(containsString("Insufficient funds")));
    }

    @Test
    @DisplayName("getTransaction_shouldReturn200_withData - GET with existing ID returns 200 with transaction data")
    void getTransaction_shouldReturn200_withData() throws Exception {
        // Arrange
        var response = createTestResponse();
        when(transactionService.getTransactionById(1L)).thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/v1/transactions/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Success"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.accountId").value(1))
                .andExpect(jsonPath("$.data.accountName").value("Test Account"))
                .andExpect(jsonPath("$.data.type").value("INCOME"))
                .andExpect(jsonPath("$.data.amount").value(100.00))
                .andExpect(jsonPath("$.data.description").value("Test description"))
                .andExpect(jsonPath("$.data.category").value("Salary"));
    }

    @Test
    @DisplayName("getTransaction_shouldReturn404_whenNotFound - GET with nonexistent ID returns 404")
    void getTransaction_shouldReturn404_whenNotFound() throws Exception {
        // Arrange
        when(transactionService.getTransactionById(999L))
                .thenThrow(new ResourceNotFoundException("Transaction not found with id: 999"));

        // Act & Assert
        mockMvc.perform(get("/api/v1/transactions/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Transaction not found with id: 999"));
    }

    @Test
    @DisplayName("getTransactions_shouldReturn200_withPagedResponse - GET returns paginated transaction list")
    void getTransactions_shouldReturn200_withPagedResponse() throws Exception {
        // Arrange
        var transactions = List.of(createTestResponse());
        var pagedResponse = new PagedResponse<>(transactions, 0, 10, 1, 1);
        when(transactionService.getTransactions(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(pagedResponse);

        // Act & Assert
        mockMvc.perform(get("/api/v1/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(10))
                .andExpect(jsonPath("$.data.totalPages").value(1));
    }

    @Test
    @DisplayName("getTransactions_shouldReturn200_withFilterParams - GET with query params passes filters")
    void getTransactions_shouldReturn200_withFilterParams() throws Exception {
        // Arrange
        var transactions = List.of(createTestResponse());
        var pagedResponse = new PagedResponse<>(transactions, 0, 10, 1, 1);
        when(transactionService.getTransactions(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(pagedResponse);

        // Act & Assert
        mockMvc.perform(get("/api/v1/transactions")
                        .param("type", "INCOME")
                        .param("category", "Salary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].type").value("INCOME"))
                .andExpect(jsonPath("$.data.content[0].category").value("Salary"));
    }

    @Test
    @DisplayName("updateTransaction_shouldReturn200_withUpdatedData - PATCH with valid body returns 200 with updated transaction")
    void updateTransaction_shouldReturn200_withUpdatedData() throws Exception {
        // Arrange
        var request = new TransactionUpdateRequest(TransactionType.EXPENSE,
                new BigDecimal("50.00"), "Updated description", LocalDate.of(2026, 2, 1), "Food");
        var updatedResponse = new TransactionResponse(1L, 1L, "Test Account", TransactionType.EXPENSE,
                new BigDecimal("50.00"), "Updated description", LocalDate.of(2026, 2, 1),
                "Food", LocalDateTime.of(2026, 1, 15, 10, 0));
        when(transactionService.updateTransaction(eq(1L), any(TransactionUpdateRequest.class)))
                .thenReturn(updatedResponse);

        // Act & Assert
        mockMvc.perform(patch("/api/v1/transactions/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Transaction updated successfully"))
                .andExpect(jsonPath("$.data.type").value("EXPENSE"))
                .andExpect(jsonPath("$.data.amount").value(50.00))
                .andExpect(jsonPath("$.data.description").value("Updated description"))
                .andExpect(jsonPath("$.data.category").value("Food"));
    }

    @Test
    @DisplayName("updateTransaction_shouldReturn404_whenNotFound - PATCH with nonexistent ID returns 404")
    void updateTransaction_shouldReturn404_whenNotFound() throws Exception {
        // Arrange
        var request = new TransactionUpdateRequest(TransactionType.EXPENSE,
                new BigDecimal("50.00"), null, null, null);
        when(transactionService.updateTransaction(eq(999L), any(TransactionUpdateRequest.class)))
                .thenThrow(new ResourceNotFoundException("Transaction not found with id: 999"));

        // Act & Assert
        mockMvc.perform(patch("/api/v1/transactions/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Transaction not found with id: 999"));
    }

    @Test
    @DisplayName("deleteTransaction_shouldReturn204_withValidId - DELETE with existing ID returns 204 No Content")
    void deleteTransaction_shouldReturn204_withValidId() throws Exception {
        // Arrange
        doNothing().when(transactionService).deleteTransaction(1L);

        // Act & Assert
        mockMvc.perform(delete("/api/v1/transactions/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("deleteTransaction_shouldReturn404_whenNotFound - DELETE with nonexistent ID returns 404")
    void deleteTransaction_shouldReturn404_whenNotFound() throws Exception {
        // Arrange
        doThrow(new ResourceNotFoundException("Transaction not found with id: 999"))
                .when(transactionService).deleteTransaction(999L);

        // Act & Assert
        mockMvc.perform(delete("/api/v1/transactions/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Transaction not found with id: 999"));
    }

    @Test
    @DisplayName("createTransaction_shouldReturn400_withInvalidEnumType - POST with invalid type returns 400 with valid values")
    void createTransaction_shouldReturn400_withInvalidEnumType() throws Exception {
        // Arrange
        String body = """
                {
                    "accountId": 1,
                    "type": "INVALID",
                    "amount": 100.00,
                    "transactionDate": "2026-01-15"
                }
                """;

        // Act & Assert
        mockMvc.perform(post("/api/v1/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid Value"))
                .andExpect(jsonPath("$.path").value("/api/v1/transactions"))
                .andExpect(jsonPath("$.message").value(containsString("INVALID")))
                .andExpect(jsonPath("$.message").value(containsString("for field 'type'")))
                .andExpect(jsonPath("$.message").value(containsString("INCOME")))
                .andExpect(jsonPath("$.message").value(containsString("EXPENSE")));
    }

    @Test
    @DisplayName("updateTransaction_shouldReturn405_whenUsingPut - PUT not allowed, only PATCH supported")
    void updateTransaction_shouldReturn405_whenUsingPut() throws Exception {
        var request = new TransactionUpdateRequest(TransactionType.EXPENSE,
                new BigDecimal("50.00"), null, null, null);
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .put("/api/v1/transactions/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    @DisplayName("updateTransaction_shouldReturn400_withInsufficientFunds - PATCH when funds insufficient returns 400")
    void updateTransaction_shouldReturn400_withInsufficientFunds() throws Exception {
        var request = new TransactionUpdateRequest(TransactionType.EXPENSE,
                new BigDecimal("99999.00"), null, null, null);
        when(transactionService.updateTransaction(eq(1L), any(TransactionUpdateRequest.class)))
                .thenThrow(new InsufficientFundsException("Insufficient funds. Current balance: 1000.00"));
        mockMvc.perform(patch("/api/v1/transactions/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value(containsString("Insufficient funds")));
    }

    @Test
    @DisplayName("createTransaction_shouldReturn400_withDecimalMinViolation - POST with amount below 0.01 returns 400")
    void createTransaction_shouldReturn400_withDecimalMinViolation() throws Exception {
        String body = """
                {
                    "accountId": 1,
                    "type": "EXPENSE",
                    "amount": 0.00,
                    "transactionDate": "2026-01-15"
                }
                """;
        mockMvc.perform(post("/api/v1/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'amount')]").exists());
    }

    @Test
    @DisplayName("createTransaction_shouldReturn400_withFutureDate - POST with future transactionDate returns 400")
    void createTransaction_shouldReturn400_withFutureDate() throws Exception {
        String body = """
                {
                    "accountId": 1,
                    "type": "INCOME",
                    "amount": 100.00,
                    "transactionDate": "2099-01-01"
                }
                """;
        mockMvc.perform(post("/api/v1/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'transactionDate')]").exists());
    }

    @Test
    @DisplayName("updateTransaction_shouldReturn400_withDecimalMinViolation - PATCH with amount below 0.01 returns 400")
    void updateTransaction_shouldReturn400_withDecimalMinViolation() throws Exception {
        String body = """
                {
                    "amount": 0.00
                }
                """;
        mockMvc.perform(patch("/api/v1/transactions/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'amount')]").exists());
    }

    @Test
    @DisplayName("updateTransaction_shouldReturn400_withFutureDate - PATCH with future transactionDate returns 400")
    void updateTransaction_shouldReturn400_withFutureDate() throws Exception {
        String body = """
                {
                    "transactionDate": "2099-01-01"
                }
                """;
        mockMvc.perform(patch("/api/v1/transactions/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'transactionDate')]").exists());
    }

    @Test
    @DisplayName("updateTransaction_shouldReturn200_withEmptyBody - PATCH with empty body returns existing transaction unchanged")
    void updateTransaction_shouldReturn200_withEmptyBody() throws Exception {
        // Arrange — empty body means all-null fields, service returns existing unchanged
        var response = createTestResponse();
        when(transactionService.updateTransaction(eq(1L), any(TransactionUpdateRequest.class)))
                .thenReturn(response);

        String body = "{}";
        mockMvc.perform(patch("/api/v1/transactions/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1));
    }
}
