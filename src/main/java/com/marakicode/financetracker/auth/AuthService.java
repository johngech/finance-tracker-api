package com.marakicode.financetracker.auth;

import com.marakicode.financetracker.common.CurrentUserProvider;
import com.marakicode.financetracker.users.UserService;
import com.marakicode.financetracker.users.dto.UserCreateRequest;
import com.marakicode.financetracker.users.dto.UserDto;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserService userService;
    private final CurrentUserProvider currentUserProvider;

    public JwtResponse login(LoginRequest request, HttpServletResponse response) {
        log.info("event=auth.login_attempt email={}", request.email());
        authenticateCredentials(request.email(), request.password());
        var user = userService.findByEmail(request.email());
        log.info("event=auth.login_success userId={}", user.getId());
        return generateTokenPair(user, response);
    }

    public UserDto register(RegisterRequest request, HttpServletResponse response) {
        log.info("event=auth.register email={}", request.email());
        var userDto = userService.createUser(new UserCreateRequest(
                request.firstName(), request.lastName(), request.email(), request.password()));
        var user = userService.findById(userDto.id());
        String refreshToken = jwtService.generateRefreshToken(user).toString();
        addRefreshTokenCookie(refreshToken, response);
        return userDto;
    }

    public JwtResponse refresh(String refreshToken) {
        var jwt = jwtService.parseToken(refreshToken);
        validateRefreshToken(jwt);
        var user = userService.findById(jwt.get().getUserId());
        log.info("event=auth.token_refreshed userId={}", user.getId());
        String accessToken = jwtService.generateAccessToken(user).toString();
        return new JwtResponse(accessToken);
    }

    @Transactional(readOnly = true)
    public UserDto me() {
        Long userId = currentUserProvider.getCurrentUserId();
        return userService.getUserById(userId);
    }

    public void logout(HttpServletResponse response) {
        log.info("event=auth.logout");
        SecurityContextHolder.clearContext();
        deleteRefreshTokenCookie(response);
    }

    private JwtResponse generateTokenPair(com.marakicode.financetracker.users.User user, HttpServletResponse response) {
        String accessToken = jwtService.generateAccessToken(user).toString();
        String refreshToken = jwtService.generateRefreshToken(user).toString();
        addRefreshTokenCookie(refreshToken, response);
        return new JwtResponse(accessToken);
    }

    private void addRefreshTokenCookie(String refreshToken, HttpServletResponse response) {
        setRefreshCookie(refreshToken, jwtService.getRefreshTokenExpiration(), response);
    }

    private void deleteRefreshTokenCookie(HttpServletResponse response) {
        setRefreshCookie("", 0, response);
    }

    private void setRefreshCookie(String value, long maxAge, HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from("refreshToken", value)
                .httpOnly(true)
                .secure(true)
                .path("/api/v1/auth")
                .maxAge(maxAge)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void validateRefreshToken(java.util.Optional<Jwt> jwtOpt) {
        if (jwtOpt.isEmpty()) {
            throw new InvalidJwtAuthenticationException("Invalid refresh token");
        }
        var jwt = jwtOpt.get();
        if (jwt.isExpired()) {
            throw new InvalidJwtAuthenticationException("Refresh token has expired");
        }
        if (!jwt.isRefreshToken()) {
            throw new InvalidJwtAuthenticationException("Token is not a refresh token");
        }
    }

    private void authenticateCredentials(String email, String password) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, password));
    }
}
