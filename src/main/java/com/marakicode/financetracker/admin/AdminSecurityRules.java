package com.marakicode.financetracker.admin;

import com.marakicode.financetracker.common.SecurityRules;
import com.marakicode.financetracker.users.Role;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.stereotype.Component;

@Component
public class AdminSecurityRules implements SecurityRules {

    @Override
    public void configure(
            AuthorizeHttpRequestsConfigurer<HttpSecurity>
                    .AuthorizationManagerRequestMatcherRegistry registry) {
        // Defense-in-depth: HTTP-level ADMIN role enforcement.
        // Complements @PreAuthorize on controllers — if a controller
        // accidentally omits @PreAuthorize, this rule still blocks access.
        registry.requestMatchers("/api/v1/admin/**").hasRole(Role.ADMIN.name());
    }
}
