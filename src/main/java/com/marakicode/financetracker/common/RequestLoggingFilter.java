package com.marakicode.financetracker.common;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Logs every inbound HTTP request with method, URI, status, and response time.
 * <p>
 * Sets {@code requestId} (UUID) and {@code userEmail} in SLF4J MDC so that
 * all subsequent log lines within the same request are correlated. Both values
 * are cleared in a {@code finally} block to prevent leakage across threads.
 * <p>
 * Static resources and OPTIONS preflight requests are skipped to reduce noise.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 100)
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final String REQUEST_ID = "requestId";
    private static final String USER_EMAIL = "userEmail";

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        if (isStaticResource(request) || "OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String requestId = UUID.randomUUID().toString();
        MDC.put(REQUEST_ID, requestId);

        long startTime = System.nanoTime();
        try {
            filterChain.doFilter(request, response);
        } finally {
            // Extract email AFTER JWT filter has populated SecurityContext
            String userEmail = extractUserEmail();
            if (userEmail != null) {
                MDC.put(USER_EMAIL, userEmail);
            }
            long durationMs = (System.nanoTime() - startTime) / 1_000_000;
            log.info("event=http.request method={} uri={} status={} durationMs={}",
                    request.getMethod(), request.getRequestURI(), response.getStatus(), durationMs);
            MDC.clear();
        }
    }

    private String extractUserEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getName() != null && !auth.getName().isBlank()) {
            return auth.getName();
        }
        return null;
    }

    private boolean isStaticResource(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri.startsWith("/static/") || uri.startsWith("/css/")
                || uri.startsWith("/js/") || uri.startsWith("/images/")
                || uri.endsWith(".ico") || uri.endsWith(".png")
                || uri.endsWith(".jpg") || uri.endsWith(".css")
                || uri.endsWith(".js");
    }
}
