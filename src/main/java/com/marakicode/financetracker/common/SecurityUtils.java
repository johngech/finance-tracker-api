package com.marakicode.financetracker.common;

import com.marakicode.financetracker.users.User;
import com.marakicode.financetracker.users.UserService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static User getCurrentUser(UserService userService) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null || auth.getName().isBlank()) {
            throw new AccessDeniedException("Not authenticated");
        }
        return userService.findByEmail(auth.getName());
    }
}
