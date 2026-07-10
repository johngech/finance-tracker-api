package com.marakicode.financetracker.auth;

import com.marakicode.financetracker.users.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final UserService userService;

    public JwtAuthenticationFilter(JwtService jwtService, @Lazy UserService userService) {
        this.jwtService = jwtService;
        this.userService = userService;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String token = extractTokenFromHeader(request);

        if (token != null) {
            attemptAuthentication(token, request);
        }

        filterChain.doFilter(request, response);
    }

    private String extractTokenFromHeader(HttpServletRequest request) {
        String authHeader = request.getHeader(AUTHORIZATION_HEADER);

        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            return null;
        }

        return authHeader.replace(BEARER_PREFIX,"");
    }

    private void attemptAuthentication(String token, HttpServletRequest request) {
        try {
            extractAndSetAuthentication(token, request);
        } catch (Exception e) {
            log.debug("Could not set user authentication: {}", e.getMessage());
        }
    }

    private void extractAndSetAuthentication(String jwtToken, HttpServletRequest request) {
        var jwt = jwtService.parseToken(jwtToken);

        if (jwt == null || jwt.isExpired()) {
            return;
        }

        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            return;
        }

        var user = userService.findById(jwt.getUserId());

        String role = jwt.getRole();
        var authorities = role != null
                ? List.of(new SimpleGrantedAuthority("ROLE_" + role))
                : Collections.<SimpleGrantedAuthority>emptyList();

        UserDetails userDetails = new org.springframework.security.core.userdetails.User(
                user.getEmail(), user.getPasswordHash(), authorities);
        setAuthentication(userDetails, request);
    }

    private void setAuthentication(UserDetails userDetails, HttpServletRequest request) {
        var authToken = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authToken);
    }
}
