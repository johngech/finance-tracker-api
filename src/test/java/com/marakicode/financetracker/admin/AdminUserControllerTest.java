package com.marakicode.financetracker.admin;

import com.marakicode.financetracker.admin.dto.PasswordResetResponse;
import com.marakicode.financetracker.admin.dto.RoleUpdateRequest;
import com.marakicode.financetracker.auth.JwtService;
import com.marakicode.financetracker.common.PagedResponse;
import com.marakicode.financetracker.common.ResourceNotFoundException;
import com.marakicode.financetracker.users.Role;
import com.marakicode.financetracker.users.dto.UserSummary;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminUserController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminUserService adminUserService;

    @MockitoBean
    private JwtService jwtService;

    private static UserSummary sampleUserSummary() {
        return new UserSummary(
                1L, "John", "Doe", "john@example.com",
                Role.USER, true, LocalDateTime.of(2025, 1, 15, 10, 30));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("listUsers_shouldReturn200_withPagedResponse")
    void listUsers_shouldReturn200_withPagedResponse() throws Exception {
        var users = List.of(sampleUserSummary());
        var paged = new PagedResponse<>(users, 0, 10, 1, 1);
        when(adminUserService.listUsers(any(), any())).thenReturn(paged);

        mockMvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].firstName").value("John"))
                .andExpect(jsonPath("$.data.content[0].email").value("john@example.com"))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.totalPages").value(1));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("getUser_shouldReturn200_withValidId")
    void getUser_shouldReturn200_withValidId() throws Exception {
        var user = sampleUserSummary();
        when(adminUserService.getUser(1L)).thenReturn(user);

        mockMvc.perform(get("/api/v1/admin/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.firstName").value("John"))
                .andExpect(jsonPath("$.data.lastName").value("Doe"))
                .andExpect(jsonPath("$.data.email").value("john@example.com"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("getUser_shouldReturn404_whenNotFound")
    void getUser_shouldReturn404_whenNotFound() throws Exception {
        when(adminUserService.getUser(999L))
                .thenThrow(new ResourceNotFoundException("User not found with id: 999"));

        mockMvc.perform(get("/api/v1/admin/users/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("suspendUser_shouldReturn200_withValidId")
    void suspendUser_shouldReturn200_withValidId() throws Exception {
        doNothing().when(adminUserService).suspendUser(1L);

        mockMvc.perform(patch("/api/v1/admin/users/1/suspend"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User suspended successfully"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("suspendUser_shouldReturn404_whenNotFound")
    void suspendUser_shouldReturn404_whenNotFound() throws Exception {
        doThrow(new ResourceNotFoundException("User not found with id: 999"))
                .when(adminUserService).suspendUser(999L);

        mockMvc.perform(patch("/api/v1/admin/users/999/suspend"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("activateUser_shouldReturn200_withValidId")
    void activateUser_shouldReturn200_withValidId() throws Exception {
        doNothing().when(adminUserService).activateUser(1L);

        mockMvc.perform(patch("/api/v1/admin/users/1/activate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User activated successfully"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("updateRole_shouldReturn200_withValidRequest")
    void updateRole_shouldReturn200_withValidRequest() throws Exception {
        var request = new RoleUpdateRequest(Role.ADMIN);
        doNothing().when(adminUserService).updateRole(eq(1L), any());

        mockMvc.perform(patch("/api/v1/admin/users/1/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"ADMIN\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User role updated successfully"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("updateRole_shouldReturn400_withNullRole")
    void updateRole_shouldReturn400_withNullRole() throws Exception {
        mockMvc.perform(patch("/api/v1/admin/users/1/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'role')]").exists());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("resetPassword_shouldReturn200_withTempPassword")
    void resetPassword_shouldReturn200_withTempPassword() throws Exception {
        var response = new PasswordResetResponse("tempPass123");
        when(adminUserService.resetPassword(1L)).thenReturn(response);

        mockMvc.perform(post("/api/v1/admin/users/1/reset-password"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.temporaryPassword").value("tempPass123"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("resetPassword_shouldReturn404_whenNotFound")
    void resetPassword_shouldReturn404_whenNotFound() throws Exception {
        when(adminUserService.resetPassword(999L))
                .thenThrow(new ResourceNotFoundException("User not found with id: 999"));

        mockMvc.perform(post("/api/v1/admin/users/999/reset-password"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("activateUser_shouldReturn404_whenNotFound")
    void activateUser_shouldReturn404_whenNotFound() throws Exception {
        doThrow(new ResourceNotFoundException("User not found with id: 999"))
            .when(adminUserService).activateUser(999L);

        mockMvc.perform(patch("/api/v1/admin/users/999/activate"))
            .andExpect(status().isNotFound());
    }
}
