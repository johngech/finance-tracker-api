package com.marakicode.financetracker.common;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;

/**
 * Permits unauthenticated access to the homepage and Swagger/OpenAPI documentation endpoints.
 */
@Configuration
@Order(0)
public class OpenApiSecurityRules implements SecurityRules {

    @Override
    public void configure(
            AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry registry) {
        registry.requestMatchers(
                "/",
                "/index.html",
                "/swagger-ui/**",
                "/v3/api-docs/**",
                "/swagger-ui.html"
        ).permitAll();
    }
}
