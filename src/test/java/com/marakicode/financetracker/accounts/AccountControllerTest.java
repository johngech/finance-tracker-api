package com.marakicode.financetracker.accounts;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marakicode.financetracker.accounts.dto.AccountCreateRequest;
import com.marakicode.financetracker.accounts.dto.AccountResponse;
import com.marakicode.financetracker.accounts.dto.CurrencyUpdateRequest;
import com.marakicode.financetracker.accounts.dto.UpdateAccountTypeRequest;
import com.marakicode.financetracker.common.DuplicateResourceException;
import com.marakicode.financetracker.common.PagedResponse;
import com.marakicode.financetracker.common.ResourceNotFoundException;
import com.marakicode.financetracker.auth.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AccountController.class)
@AutoConfigureMockMvc(addFilters = false)
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AccountService accountService;

    @MockitoBean
    private JwtService jwtService;

    private static AccountResponse sampleAccountResponse() {
        return new AccountResponse(1L, "Checking123", AccountType.CHECKING,
                new BigDecimal("1000.00"), "USD", LocalDateTime.of(2025, 1, 15, 10, 30));
    }

    @Test
    @DisplayName("createAccount_shouldReturn201_withValidRequest - POST with valid body returns 201 with account data")
    void createAccount_shouldReturn201_withValidRequest() throws Exception {
        // Arrange
        var request = new AccountCreateRequest("Checking123", AccountType.CHECKING, "USD", new BigDecimal("1000.00"));
        var response = sampleAccountResponse();
        when(accountService.createAccount(any(AccountCreateRequest.class))).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Account created successfully"))
                .andExpect(jsonPath("$.data.name").value("Checking123"))
                .andExpect(jsonPath("$.data.type").value("CHECKING"))
                .andExpect(jsonPath("$.data.balance").value(1000.00))
                .andExpect(jsonPath("$.data.currency").value("USD"));
    }

    @Test
    @DisplayName("createAccount_shouldReturn400_withInvalidRequest - POST with missing fields returns 400 with field errors")
    void createAccount_shouldReturn400_withInvalidRequest() throws Exception {
        // Arrange
        var invalidRequest = new AccountCreateRequest("", null, "", null);

        // Act & Assert
        mockMvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.fieldErrors").isArray())
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'name')]").exists())
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'type')]").exists())
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'currency')]").exists())
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'initialBalance')]").exists());
    }

    @Test
    @DisplayName("createAccount_shouldReturn409_withDuplicateName - POST with duplicate name returns 409 Conflict")
    void createAccount_shouldReturn409_withDuplicateName() throws Exception {
        // Arrange
        var request = new AccountCreateRequest("Checking123", AccountType.CHECKING, "USD", new BigDecimal("1000.00"));
        when(accountService.createAccount(any(AccountCreateRequest.class)))
                .thenThrow(new DuplicateResourceException("Account with name 'Checking123' already exists"));

        // Act & Assert
        mockMvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Account with name 'Checking123' already exists"));
    }

    @Test
    @DisplayName("getAccount_shouldReturn200_withValidId - GET with existing ID returns 200 with account")
    void getAccount_shouldReturn200_withValidId() throws Exception {
        // Arrange
        var response = sampleAccountResponse();
        when(accountService.getAccountById(1L)).thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/v1/accounts/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("Checking123"))
                .andExpect(jsonPath("$.data.type").value("CHECKING"))
                .andExpect(jsonPath("$.data.balance").value(1000.00))
                .andExpect(jsonPath("$.data.currency").value("USD"));
    }

    @Test
    @DisplayName("getAccount_shouldReturn404_whenNotFound - GET with nonexistent ID returns 404")
    void getAccount_shouldReturn404_whenNotFound() throws Exception {
        // Arrange
        when(accountService.getAccountById(999L))
                .thenThrow(new ResourceNotFoundException("Account not found with id: 999"));

        // Act & Assert
        mockMvc.perform(get("/api/v1/accounts/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("getAccounts_shouldReturn200_withPagedResponse - GET returns paginated account list")
    void getAccounts_shouldReturn200_withPagedResponse() throws Exception {
        // Arrange
        var accounts = List.of(sampleAccountResponse());
        var pagedResponse = new PagedResponse<>(accounts, 0, 10, 1, 1);
        when(accountService.getAccounts(any(), any(), any(), any())).thenReturn(pagedResponse);

        // Act & Assert
        mockMvc.perform(get("/api/v1/accounts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(10))
                .andExpect(jsonPath("$.data.totalPages").value(1));
    }

    @Test
    @DisplayName("updateAccount_shouldReturn200_withValidRequest - PATCH with valid body returns 200 with updated account")
    void updateAccount_shouldReturn200_withValidRequest() throws Exception {
        // Arrange
        var request = new CurrencyUpdateRequest("EUR");
        var updatedResponse = new AccountResponse(1L, "Checking123", AccountType.CHECKING,
                new BigDecimal("1000.00"), "EUR", LocalDateTime.of(2025, 1, 15, 10, 30));
        when(accountService.updateAccount(eq(1L), any(CurrencyUpdateRequest.class))).thenReturn(updatedResponse);

        // Act & Assert
        mockMvc.perform(patch("/api/v1/accounts/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Currency updated successfully"))
                .andExpect(jsonPath("$.data.name").value("Checking123"))
                .andExpect(jsonPath("$.data.currency").value("EUR"));
    }

    @Test
    @DisplayName("updateAccount_shouldReturn404_whenNotFound - PATCH with nonexistent ID returns 404")
    void updateAccount_shouldReturn404_whenNotFound() throws Exception {
        // Arrange
        var request = new CurrencyUpdateRequest("EUR");
        when(accountService.updateAccount(eq(999L), any(CurrencyUpdateRequest.class)))
                .thenThrow(new ResourceNotFoundException("Account not found with id: 999"));

        // Act & Assert
        mockMvc.perform(patch("/api/v1/accounts/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("updateAccountType_shouldReturn200_withValidRequest - PATCH /{id}/type with admin role returns 200")
    void updateAccountType_shouldReturn200_withValidRequest() throws Exception {
        // Arrange
        var request = new UpdateAccountTypeRequest(AccountType.SAVINGS);
        var updatedResponse = new AccountResponse(1L, "Checking123", AccountType.SAVINGS,
                new BigDecimal("1000.00"), "USD", LocalDateTime.of(2025, 1, 15, 10, 30));
        when(accountService.updateAccountType(eq(1L), any(UpdateAccountTypeRequest.class))).thenReturn(updatedResponse);

        // Act & Assert
        mockMvc.perform(patch("/api/v1/accounts/1/type")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Account type updated successfully"))
                .andExpect(jsonPath("$.data.type").value("SAVINGS"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("updateAccountType_shouldReturn400_withNullType - PATCH /{id}/type with null type returns 400")
    void updateAccountType_shouldReturn400_withNullType() throws Exception {
        // Arrange
        var request = new UpdateAccountTypeRequest(null);

        // Act & Assert
        mockMvc.perform(patch("/api/v1/accounts/1/type")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'type')]").exists());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("updateAccountType_shouldReturn404_whenNotFound - PATCH /{id}/type with nonexistent ID returns 404")
    void updateAccountType_shouldReturn404_whenNotFound() throws Exception {
        // Arrange
        var request = new UpdateAccountTypeRequest(AccountType.SAVINGS);
        when(accountService.updateAccountType(eq(999L), any(UpdateAccountTypeRequest.class)))
                .thenThrow(new ResourceNotFoundException("Account not found with id: 999"));

        // Act & Assert
        mockMvc.perform(patch("/api/v1/accounts/999/type")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    @DisplayName("deleteAccount_shouldReturn204_withValidId - DELETE with existing ID returns 204 No Content")
    void deleteAccount_shouldReturn204_withValidId() throws Exception {
        // Arrange
        doNothing().when(accountService).deleteAccount(1L);

        // Act & Assert
        mockMvc.perform(delete("/api/v1/accounts/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("deleteAccount_shouldReturn404_whenNotFound - DELETE with nonexistent ID returns 404")
    void deleteAccount_shouldReturn404_whenNotFound() throws Exception {
        // Arrange
        doThrow(new ResourceNotFoundException("Account not found with id: 999"))
                .when(accountService).deleteAccount(999L);

        // Act & Assert
        mockMvc.perform(delete("/api/v1/accounts/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Account not found with id: 999"));
    }

    @Test
    @DisplayName("updateAccount_shouldReturn405_whenUsingPut - PUT is not allowed on accounts endpoint")
    void updateAccount_shouldReturn405_whenUsingPut() throws Exception {
        // Act & Assert
        mockMvc.perform(put("/api/v1/accounts/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currency\":\"USD\"}"))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    @DisplayName("createAccount_shouldReturn400_withInvalidEnumType - POST with unknown type returns 400 with valid values")
    void createAccount_shouldReturn400_withInvalidEnumType() throws Exception {
        String body = """
                {
                    "name": "Checking123",
                    "type": "INVALID",
                    "currency": "USD",
                    "initialBalance": 1000.00
                }
                """;

        mockMvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid Value"))
                .andExpect(jsonPath("$.path").value("/api/v1/accounts"))
                .andExpect(jsonPath("$.message").value(containsString("INVALID")))
                .andExpect(jsonPath("$.message").value(containsString("for field 'type'")))
                .andExpect(jsonPath("$.message").value(containsString("CHECKING")))
                .andExpect(jsonPath("$.message").value(containsString("SAVINGS")))
                .andExpect(jsonPath("$.message").value(containsString("INVESTMENT")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("updateAccountType_shouldReturn400_withInvalidEnumType - PATCH /{id}/type with unknown type returns 400 with valid values")
    void updateAccountType_shouldReturn400_withInvalidEnumType() throws Exception {
        String body = """
                {
                    "type": "BROKERAGE"
                }
                """;

        mockMvc.perform(patch("/api/v1/accounts/1/type")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid Value"))
                .andExpect(jsonPath("$.path").value("/api/v1/accounts/1/type"))
                .andExpect(jsonPath("$.message").value(containsString("BROKERAGE")))
                .andExpect(jsonPath("$.message").value(containsString("for field 'type'")))
                .andExpect(jsonPath("$.message").value(containsString("CHECKING")))
                .andExpect(jsonPath("$.message").value(containsString("SAVINGS")))
                .andExpect(jsonPath("$.message").value(containsString("INVESTMENT")));
    }

    @Test
    @DisplayName("createAccount_shouldReturn400_withLowercaseEnumType - POST with lowercase type returns 400 (case-sensitive)")
    void createAccount_shouldReturn400_withLowercaseEnumType() throws Exception {
        String body = """
                {
                    "name": "Checking123",
                    "type": "checking",
                    "currency": "USD",
                    "initialBalance": 1000.00
                }
                """;

        mockMvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid Value"))
                .andExpect(jsonPath("$.message").value(containsString("checking")))
                .andExpect(jsonPath("$.message").value(containsString("for field 'type'")))
                .andExpect(jsonPath("$.message").value(containsString("CHECKING")));
    }

    @Test
    @DisplayName("getMyAccounts_shouldReturn200_withPagedResponse - GET /mine returns paginated accounts for authenticated user")
    void getMyAccounts_shouldReturn200_withPagedResponse() throws Exception {
        // Arrange
        var accounts = List.of(sampleAccountResponse());
        var pagedResponse = new PagedResponse<>(accounts, 0, 10, 1, 1);
        when(accountService.getAccounts(any(), any(), any(), any())).thenReturn(pagedResponse);

        // Act & Assert
        mockMvc.perform(get("/api/v1/accounts/mine"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].name").value("Checking123"))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(10))
                .andExpect(jsonPath("$.data.totalPages").value(1));
    }

    @Test
    @DisplayName("getMyAccounts_shouldReturn200_withFilters - GET /mine with query params passes filters to service")
    void getMyAccounts_shouldReturn200_withFilters() throws Exception {
        // Arrange
        var pagedResponse = new PagedResponse<AccountResponse>(List.of(), 0, 10, 0, 0);
        when(accountService.getAccounts(any(), any(), any(), any())).thenReturn(pagedResponse);

        // Act & Assert
        mockMvc.perform(get("/api/v1/accounts/mine")
                        .param("search", "Savings")
                        .param("type", "SAVINGS")
                        .param("currency", "EUR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content.length()").value(0));
    }

    @Test
    @DisplayName("getMyAccounts_shouldReturn200_withEmptyResult - GET /mine with no accounts returns empty page")
    void getMyAccounts_shouldReturn200_withEmptyResult() throws Exception {
        // Arrange
        var pagedResponse = new PagedResponse<AccountResponse>(List.of(), 0, 10, 0, 0);
        when(accountService.getAccounts(any(), any(), any(), any())).thenReturn(pagedResponse);

        // Act & Assert
        mockMvc.perform(get("/api/v1/accounts/mine"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content.length()").value(0))
                .andExpect(jsonPath("$.data.totalPages").value(0));
    }

    @Test
    @DisplayName("createAccount_shouldReturn400_withEmptyStringEnumType - POST with empty string type returns 400")
    void createAccount_shouldReturn400_withEmptyStringEnumType() throws Exception {
        String body = """
                {
                    "name": "Checking123",
                    "type": "",
                    "currency": "USD",
                    "initialBalance": 1000.00
                }
                """;

        mockMvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }
}
