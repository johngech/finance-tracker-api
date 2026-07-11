package com.marakicode.financetracker.admin;

import com.marakicode.financetracker.accounts.AccountType;
import com.marakicode.financetracker.accounts.dto.AccountSummary;
import com.marakicode.financetracker.auth.JwtService;
import com.marakicode.financetracker.common.PagedResponse;
import com.marakicode.financetracker.common.ResourceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminAccountController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminAccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminAccountService adminAccountService;

    @MockitoBean
    private JwtService jwtService;

    private static AccountSummary sampleAccountSummary() {
        return new AccountSummary(
                1L, 10L, "John Doe", "Checking",
                AccountType.CHECKING, new BigDecimal("5000.00"),
                "USD", false, LocalDateTime.of(2025, 1, 15, 10, 30));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("listAccounts_shouldReturn200_withPagedResponse")
    void listAccounts_shouldReturn200_withPagedResponse() throws Exception {
        var accounts = List.of(sampleAccountSummary());
        var paged = new PagedResponse<>(accounts, 0, 10, 1, 1);
        when(adminAccountService.listAccounts(any(), any())).thenReturn(paged);

        mockMvc.perform(get("/api/v1/admin/accounts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].name").value("Checking"))
                .andExpect(jsonPath("$.data.content[0].type").value("CHECKING"))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.totalPages").value(1));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("getAccount_shouldReturn200_withValidId")
    void getAccount_shouldReturn200_withValidId() throws Exception {
        var account = sampleAccountSummary();
        when(adminAccountService.getAccount(1L)).thenReturn(account);

        mockMvc.perform(get("/api/v1/admin/accounts/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("Checking"))
                .andExpect(jsonPath("$.data.type").value("CHECKING"))
                .andExpect(jsonPath("$.data.balance").value(5000.00));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("getAccount_shouldReturn404_whenNotFound")
    void getAccount_shouldReturn404_whenNotFound() throws Exception {
        when(adminAccountService.getAccount(999L))
                .thenThrow(new ResourceNotFoundException("Account not found with id: 999"));

        mockMvc.perform(get("/api/v1/admin/accounts/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("freezeAccount_shouldReturn200_withValidId")
    void freezeAccount_shouldReturn200_withValidId() throws Exception {
        doNothing().when(adminAccountService).freezeAccount(1L);

        mockMvc.perform(patch("/api/v1/admin/accounts/1/freeze"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Account frozen successfully"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("freezeAccount_shouldReturn404_whenNotFound")
    void freezeAccount_shouldReturn404_whenNotFound() throws Exception {
        doThrow(new ResourceNotFoundException("Account not found with id: 999"))
                .when(adminAccountService).freezeAccount(999L);

        mockMvc.perform(patch("/api/v1/admin/accounts/999/freeze"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("unfreezeAccount_shouldReturn200_withValidId")
    void unfreezeAccount_shouldReturn200_withValidId() throws Exception {
        doNothing().when(adminAccountService).unfreezeAccount(1L);

        mockMvc.perform(patch("/api/v1/admin/accounts/1/unfreeze"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Account unfrozen successfully"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("unfreezeAccount_shouldReturn404_whenNotFound")
    void unfreezeAccount_shouldReturn404_whenNotFound() throws Exception {
        doThrow(new ResourceNotFoundException("Account not found with id: 999"))
                .when(adminAccountService).unfreezeAccount(999L);

        mockMvc.perform(patch("/api/v1/admin/accounts/999/unfreeze"))
                .andExpect(status().isNotFound());
    }
}
