package com.marakicode.financetracker.users;

import com.marakicode.financetracker.common.SecurityRules;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.stereotype.Component;

@Component
public class UserSecurityRules implements SecurityRules {

    @Override
    public void configure(
            AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry registry) {
        // No permitAll rules — all user endpoints require authentication.
        // POST /api/v1/auth/register is the unauthenticated user creation path.
    }
}
