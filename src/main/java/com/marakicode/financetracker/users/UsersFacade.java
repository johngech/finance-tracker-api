package com.marakicode.financetracker.users;

import com.marakicode.financetracker.common.PagedResponse;
import com.marakicode.financetracker.users.dto.UserStatistics;
import com.marakicode.financetracker.users.dto.UserSummary;
import org.springframework.data.domain.Pageable;

public interface UsersFacade {
    UserSummary getUserById(Long userId);
    PagedResponse<UserSummary> listUsers(String search, Pageable pageable);
    UserStatistics getStatistics();
    void suspendUser(Long userId);
    void activateUser(Long userId);
    String resetPassword(Long userId);
    void updateUserRole(Long userId, Role role);
}
