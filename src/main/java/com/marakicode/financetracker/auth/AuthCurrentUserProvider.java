package com.marakicode.financetracker.auth;

import com.marakicode.financetracker.common.CurrentUserProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class AuthCurrentUserProvider implements CurrentUserProvider {

    @Override
    public Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserIdPrincipal principal)) {
            throw new NotAuthenticatedException("Not authenticated");
        }
        return principal.id();
    }
}
