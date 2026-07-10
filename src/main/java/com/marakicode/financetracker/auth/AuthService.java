package com.marakicode.financetracker.auth;

import com.marakicode.financetracker.common.ResourceNotFoundException;
import com.marakicode.financetracker.users.UserDto;
import com.marakicode.financetracker.users.UserService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserService userService;

    public JwtResponse login(LoginRequest request, HttpServletResponse response) {
        authenticateCredentials(request.email(), request.password());
        var user = userService.findByEmail(request.email());
        return generateTokenPair(user, response);
    }

    public UserDto register(RegisterRequest request, HttpServletResponse response) {
        var userDto = userService.createUser(new com.marakicode.financetracker.users.UserCreateRequest(
                request.firstName(), request.lastName(), request.email(), request.password()));
        var user = userService.findByEmail(request.email());
        String refreshToken = jwtService.generateRefreshToken(user).toString();
        addRefreshTokenCookie(refreshToken, response);
        return userDto;
    }

    public JwtResponse refresh(String refreshToken) {
        var jwt = jwtService.parseToken(refreshToken);
        validateRefreshToken(jwt);
        var user = userService.findById(jwt.getUserId());
        String accessToken = jwtService.generateAccessToken(user).toString();
        return new JwtResponse(accessToken);
    }

    @Transactional(readOnly = true)
    public UserDto me() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            throw new ResourceNotFoundException("Not authenticated");
        }
        String email = auth.getName();
        return userService.getUserByEmail(email);
    }

    private JwtResponse generateTokenPair(com.marakicode.financetracker.users.User user, HttpServletResponse response) {
        String accessToken = jwtService.generateAccessToken(user).toString();
        String refreshToken = jwtService.generateRefreshToken(user).toString();
        addRefreshTokenCookie(refreshToken, response);
        return new JwtResponse(accessToken);
    }

    private void addRefreshTokenCookie(String refreshToken, HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(true)
                .sameSite("Lax")
                .path("/api/v1/auth")
                .maxAge(jwtService.getRefreshTokenExpiration())
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void validateRefreshToken(Jwt jwt) {
        if (jwt == null) {
            throw new InvalidJwtAuthenticationException("Invalid refresh token");
        }
        if (jwt.isExpired()) {
            throw new InvalidJwtAuthenticationException("Refresh token has expired");
        }
    }

    private void authenticateCredentials(String email, String password) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, password));
    }
}
