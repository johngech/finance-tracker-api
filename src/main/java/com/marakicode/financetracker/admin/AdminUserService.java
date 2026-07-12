package com.marakicode.financetracker.admin;

import com.marakicode.financetracker.admin.dto.PasswordResetResponse;
import com.marakicode.financetracker.admin.dto.RoleUpdateRequest;
import com.marakicode.financetracker.common.PagedResponse;
import com.marakicode.financetracker.users.dto.UserStatistics;
import com.marakicode.financetracker.users.dto.UserSummary;
import com.marakicode.financetracker.users.UsersFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Slf4j
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
        log.info("event=admin.user_suspended userId={}", userId);
    }

    public void activateUser(Long userId) {
        usersFacade.activateUser(userId);
        log.info("event=admin.user_activated userId={}", userId);
    }

    public PasswordResetResponse resetPassword(Long userId) {
        String tempPassword = usersFacade.resetPassword(userId);
        log.info("event=admin.password_reset userId={}", userId);
        return new PasswordResetResponse(tempPassword);
    }

    public void updateRole(Long userId, RoleUpdateRequest request) {
        usersFacade.updateUserRole(userId, request.role());
        log.info("event=admin.role_updated userId={} role={}", userId, request.role());
    }

    public UserStatistics getStatistics() {
        return usersFacade.getStatistics();
    }
}
