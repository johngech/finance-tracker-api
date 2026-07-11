package com.marakicode.financetracker.common;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.stereotype.Component;

@Component
public class ActuatorSecurityRules implements SecurityRules {

    @Override
    public void configure(
            AuthorizeHttpRequestsConfigurer<HttpSecurity>
                    .AuthorizationManagerRequestMatcherRegistry registry) {
        registry.requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .requestMatchers("/actuator/**").authenticated();
    }
}
