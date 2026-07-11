package com.marakicode.financetracker.admin;

import com.marakicode.financetracker.admin.dto.PasswordResetResponse;
import com.marakicode.financetracker.admin.dto.RoleUpdateRequest;
import com.marakicode.financetracker.common.PagedResponse;
import com.marakicode.financetracker.users.dto.UserStatistics;
import com.marakicode.financetracker.users.dto.UserSummary;
import com.marakicode.financetracker.users.UsersFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UsersFacade usersFacade;

    public PagedResponse<UserSummary> listUsers(String search, Pageable pageable) {
        return usersFacade.listUsers(search, pageable);
    }

    public UserSummary getUser(Long userId) {
        return usersFacade.getUserById(userId);
    }

    public void suspendUser(Long userId) {
        usersFacade.suspendUser(userId);
    }

    public void activateUser(Long userId) {
        usersFacade.activateUser(userId);
    }

    public PasswordResetResponse resetPassword(Long userId) {
        String tempPassword = usersFacade.resetPassword(userId);
        return new PasswordResetResponse(tempPassword);
    }

    public void updateRole(Long userId, RoleUpdateRequest request) {
        usersFacade.updateUserRole(userId, request.role());
    }

    public UserStatistics getStatistics() {
        return usersFacade.getStatistics();
    }
}
