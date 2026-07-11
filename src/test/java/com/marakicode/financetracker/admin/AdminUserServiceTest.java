package com.marakicode.financetracker.admin;

import com.marakicode.financetracker.admin.dto.PasswordResetResponse;
import com.marakicode.financetracker.admin.dto.RoleUpdateRequest;
import com.marakicode.financetracker.common.PagedResponse;
import com.marakicode.financetracker.common.ResourceNotFoundException;
import com.marakicode.financetracker.users.Role;
import com.marakicode.financetracker.users.dto.UserStatistics;
import com.marakicode.financetracker.users.dto.UserSummary;
import com.marakicode.financetracker.users.UsersFacade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    @Mock
    private UsersFacade usersFacade;

    @InjectMocks
    private AdminUserService adminUserService;

    private static UserSummary sampleUserSummary() {
        return new UserSummary(
                1L, "John", "Doe", "john@example.com",
                Role.USER, true, LocalDateTime.now());
    }

    private static UserStatistics sampleStats() {
        return new UserStatistics(100, 90, 10, 5, 95);
    }

    @BeforeEach
    void setUp() {
        lenient().when(usersFacade.getStatistics()).thenReturn(sampleStats());
    }

    @Test
    @DisplayName("listUsers_delegatesToFacade")
    void listUsers_delegatesToFacade() {
        var pageable = PageRequest.of(0, 10);
        var expected = new PagedResponse<>(
                List.of(sampleUserSummary()), 0, 10, 1, 1);
        when(usersFacade.listUsers("john", pageable)).thenReturn(expected);

        var result = adminUserService.listUsers("john", pageable);

        assertThat(result).isEqualTo(expected);
        verify(usersFacade).listUsers("john", pageable);
    }

    @Test
    @DisplayName("getUser_delegatesToFacade")
    void getUser_delegatesToFacade() {
        var expected = sampleUserSummary();
        when(usersFacade.getUserById(1L)).thenReturn(expected);

        var result = adminUserService.getUser(1L);

        assertThat(result).isEqualTo(expected);
        verify(usersFacade).getUserById(1L);
    }

    @Test
    @DisplayName("suspendUser_delegatesToFacade")
    void suspendUser_delegatesToFacade() {
        adminUserService.suspendUser(1L);

        verify(usersFacade).suspendUser(1L);
    }

    @Test
    @DisplayName("activateUser_delegatesToFacade")
    void activateUser_delegatesToFacade() {
        adminUserService.activateUser(1L);

        verify(usersFacade).activateUser(1L);
    }

    @Test
    @DisplayName("resetPassword_delegatesToFacade_returnsPasswordResetResponse")
    void resetPassword_delegatesToFacade_returnsPasswordResetResponse() {
        when(usersFacade.resetPassword(1L)).thenReturn("tempPass123");

        var result = adminUserService.resetPassword(1L);

        assertThat(result).isNotNull();
        assertThat(result.temporaryPassword()).isEqualTo("tempPass123");
        verify(usersFacade).resetPassword(1L);
    }

    @Test
    @DisplayName("updateRole_delegatesToFacade")
    void updateRole_delegatesToFacade() {
        var request = new RoleUpdateRequest(Role.ADMIN);

        adminUserService.updateRole(1L, request);

        verify(usersFacade).updateUserRole(1L, Role.ADMIN);
    }

    @Test
    @DisplayName("getStatistics_delegatesToFacade")
    void getStatistics_delegatesToFacade() {
        var result = adminUserService.getStatistics();

        assertThat(result).isEqualTo(sampleStats());
        verify(usersFacade).getStatistics();
    }

    @Test
    @DisplayName("listUsers_propagatesException")
    void listUsers_propagatesException() {
        var pageable = PageRequest.of(0, 10);
        when(usersFacade.listUsers(any(), any()))
                .thenThrow(new ResourceNotFoundException("Not found"));

        assertThatThrownBy(() -> adminUserService.listUsers("x", pageable))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("getUser_propagatesResourceNotFoundException")
    void getUser_propagatesResourceNotFoundException() {
        when(usersFacade.getUserById(999L))
            .thenThrow(new ResourceNotFoundException("User not found"));
        assertThatThrownBy(() -> adminUserService.getUser(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("User not found");
    }

    @Test
    @DisplayName("suspendUser_propagatesResourceNotFoundException")
    void suspendUser_propagatesResourceNotFoundException() {
        doThrow(new ResourceNotFoundException("User not found"))
            .when(usersFacade).suspendUser(999L);
        assertThatThrownBy(() -> adminUserService.suspendUser(999L))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("activateUser_propagatesResourceNotFoundException")
    void activateUser_propagatesResourceNotFoundException() {
        doThrow(new ResourceNotFoundException("User not found"))
            .when(usersFacade).activateUser(999L);
        assertThatThrownBy(() -> adminUserService.activateUser(999L))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("resetPassword_propagatesResourceNotFoundException")
    void resetPassword_propagatesResourceNotFoundException() {
        when(usersFacade.resetPassword(999L))
            .thenThrow(new ResourceNotFoundException("User not found"));
        assertThatThrownBy(() -> adminUserService.resetPassword(999L))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("updateRole_propagatesResourceNotFoundException")
    void updateRole_propagatesResourceNotFoundException() {
        doThrow(new ResourceNotFoundException("User not found"))
            .when(usersFacade).updateUserRole(999L, Role.ADMIN);
        assertThatThrownBy(() -> adminUserService.updateRole(999L, new RoleUpdateRequest(Role.ADMIN)))
            .isInstanceOf(ResourceNotFoundException.class);
    }
}
