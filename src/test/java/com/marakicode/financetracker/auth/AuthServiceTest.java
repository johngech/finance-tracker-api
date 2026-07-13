package com.marakicode.financetracker.auth;

import com.marakicode.financetracker.common.ResourceNotFoundException;
import com.marakicode.financetracker.users.User;
import com.marakicode.financetracker.users.dto.UserCreateRequest;
import com.marakicode.financetracker.users.dto.UserDto;
import com.marakicode.financetracker.users.UserService;
import com.marakicode.financetracker.users.Role;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private UserService userService;

    @Mock
    private HttpServletResponse httpResponse;

    @InjectMocks
    private AuthService authService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private User testUser() {
        User user = new User();
        user.setId(1L);
        user.setEmail("alice@example.com");
        user.setPasswordHash("encodedPassword");
        return user;
    }

    private Jwt testRefreshJwt(Long userId) {
        io.jsonwebtoken.Claims claims = io.jsonwebtoken.Jwts.claims()
                .subject(userId.toString())
                .add("email", "alice@example.com")
                .add("role", "USER")
                .add("type", "refresh")
                .expiration(new Date(System.currentTimeMillis() + 900000))
                .build();
        javax.crypto.SecretKey key = io.jsonwebtoken.security.Keys.hmacShaKeyFor(
                "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970"
                        .getBytes(StandardCharsets.UTF_8));
        return new Jwt(key, claims);
    }

    private Jwt expiredTestRefreshJwt(Long userId) {
        io.jsonwebtoken.Claims claims = io.jsonwebtoken.Jwts.claims()
                .subject(userId.toString())
                .add("email", "alice@example.com")
                .add("role", "USER")
                .add("type", "refresh")
                .expiration(new Date(System.currentTimeMillis() - 1000))
                .build();
        javax.crypto.SecretKey key = io.jsonwebtoken.security.Keys.hmacShaKeyFor(
                "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970"
                        .getBytes(StandardCharsets.UTF_8));
        return new Jwt(key, claims);
    }

    private Jwt testAccessJwt(Long userId) {
        io.jsonwebtoken.Claims claims = io.jsonwebtoken.Jwts.claims()
                .subject(userId.toString())
                .add("email", "alice@example.com")
                .add("role", "USER")
                .add("type", "access")
                .expiration(new Date(System.currentTimeMillis() + 900000))
                .build();
        javax.crypto.SecretKey key = io.jsonwebtoken.security.Keys.hmacShaKeyFor(
                "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970"
                        .getBytes(StandardCharsets.UTF_8));
        return new Jwt(key, claims);
    }

    @Test
    @DisplayName("login should return JwtResponse with access token and set refresh token cookie when valid credentials are provided")
    void login_shouldReturnJwtResponse_whenValidCredentials() {

        // Arrange
        var loginRequest = new LoginRequest("alice@example.com", "Secret123!");
        var user = testUser();
        var accessJwt = testAccessJwt(1L);
        var refreshJwt = testRefreshJwt(1L);

        when(userService.findByEmail("alice@example.com")).thenReturn(user);
        when(jwtService.generateAccessToken(user)).thenReturn(accessJwt);
        when(jwtService.generateRefreshToken(user)).thenReturn(refreshJwt);
        when(jwtService.getRefreshTokenExpiration()).thenReturn(604800000L);

        // Act
        var result = authService.login(loginRequest, httpResponse);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.accessToken()).isNotNull().isNotBlank();
        verify(authenticationManager).authenticate(any());
        verify(jwtService).generateAccessToken(user);
        verify(jwtService).generateRefreshToken(user);
        verify(httpResponse).addHeader(any(), any());
    }

    @Test
    @DisplayName("login should propagate BadCredentialsException when invalid credentials are provided")
    void login_shouldThrow_whenInvalidCredentials() {

        // Arrange
        var loginRequest = new LoginRequest("alice@example.com", "WrongPassword!");

        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        // Act & Assert
        assertThatThrownBy(() -> authService.login(loginRequest, httpResponse))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Bad credentials");
    }

    @Test
    @DisplayName("register should return UserDto when valid request is provided")
    void register_shouldReturnUserDto_whenValidRequest() {

        // Arrange
        var registerRequest = new RegisterRequest("Alice", "Smith", "alice@example.com", "Secret123!");
        var user = testUser();
        var userDto = new UserDto(1L, "Alice", "Smith", "alice@example.com", Role.USER, LocalDateTime.now());
        var refreshJwt = testRefreshJwt(1L);

        when(userService.createUser(any(UserCreateRequest.class)))
                .thenReturn(userDto);
        when(userService.findByEmail("alice@example.com")).thenReturn(user);
        when(jwtService.generateRefreshToken(user)).thenReturn(refreshJwt);
        when(jwtService.getRefreshTokenExpiration()).thenReturn(604800000L);

        // Act
        var result = authService.register(registerRequest, httpResponse);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.firstName()).isEqualTo("Alice");
        assertThat(result.lastName()).isEqualTo("Smith");
        assertThat(result.email()).isEqualTo("alice@example.com");
        verify(userService).createUser(any(UserCreateRequest.class));
        verify(httpResponse).addHeader(any(), any());
    }

    @Test
    @DisplayName("refresh should return JwtResponse with new access token when valid refresh token string is provided")
    void refresh_shouldReturnJwtResponse_whenValidRefreshToken() {

        // Arrange
        var user = testUser();
        var jwt = testRefreshJwt(1L);
        var newAccessJwt = testAccessJwt(1L);

        when(jwtService.parseToken("valid.refresh.token")).thenReturn(Optional.of(jwt));
        when(userService.findById(1L)).thenReturn(user);
        when(jwtService.generateAccessToken(user)).thenReturn(newAccessJwt);

        // Act
        var result = authService.refresh("valid.refresh.token");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.accessToken()).isNotNull().isNotBlank();
        verify(jwtService).parseToken("valid.refresh.token");
        verify(jwtService).generateAccessToken(user);
    }

    @Test
    @DisplayName("refresh should throw InvalidJwtAuthenticationException when refresh token is invalid")
    void refresh_shouldThrow_whenInvalidRefreshToken() {

        // Arrange
        when(jwtService.parseToken("invalid.refresh.token")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> authService.refresh("invalid.refresh.token"))
                .isInstanceOf(InvalidJwtAuthenticationException.class)
                .hasMessageContaining("Invalid refresh token");
    }

    @Test
    @DisplayName("refresh should throw InvalidJwtAuthenticationException when refresh token is expired")
    void refresh_shouldThrow_whenExpiredRefreshToken() {

        // Arrange
        var expiredJwt = expiredTestRefreshJwt(1L);
        when(jwtService.parseToken("expired.refresh.token")).thenReturn(Optional.of(expiredJwt));

        // Act & Assert
        assertThatThrownBy(() -> authService.refresh("expired.refresh.token"))
                .isInstanceOf(InvalidJwtAuthenticationException.class)
                .hasMessageContaining("Refresh token has expired");
    }

    @Test
    @DisplayName("refresh should throw InvalidJwtAuthenticationException when access token is used as refresh token")
    void refresh_shouldThrow_whenAccessTokenUsedAsRefreshToken() {

        // Arrange
        var accessJwt = testAccessJwt(1L);
        when(jwtService.parseToken("access.token.used.as.refresh")).thenReturn(Optional.of(accessJwt));

        // Act & Assert
        assertThatThrownBy(() -> authService.refresh("access.token.used.as.refresh"))
                .isInstanceOf(InvalidJwtAuthenticationException.class)
                .hasMessageContaining("Token is not a refresh token");
    }

    @Test
    @DisplayName("me should return UserDto when authenticated user exists in database")
    void me_shouldReturnUserDto_whenAuthenticated() {

        // Arrange
        var user = testUser();
        var userDto = new UserDto(1L, "Alice", "Smith", "alice@example.com", Role.USER,
                LocalDateTime.of(2025, 1, 15, 10, 30));

        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        Authentication authentication = org.mockito.Mockito.mock(Authentication.class);
        when(authentication.getName()).thenReturn("alice@example.com");
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);

        when(userService.getUserByEmail("alice@example.com")).thenReturn(userDto);

        // Act
        var result = authService.me();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.email()).isEqualTo("alice@example.com");
        assertThat(result.firstName()).isEqualTo("Alice");
    }

    @Test
    @DisplayName("me should throw ResourceNotFoundException when authenticated user email not found in database")
    void me_shouldThrow_whenEmailNotFound() {

        // Arrange
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        Authentication authentication = org.mockito.Mockito.mock(Authentication.class);
        when(authentication.getName()).thenReturn("nonexistent@example.com");
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);

        when(userService.getUserByEmail("nonexistent@example.com"))
                .thenThrow(new ResourceNotFoundException("User not found with email: nonexistent@example.com"));

        // Act & Assert
        assertThatThrownBy(() -> authService.me())
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found with email: nonexistent@example.com");
    }

    @Test
    @DisplayName("me should throw NotAuthenticatedException when authentication is null")
    void me_shouldThrowNotAuthenticated_whenAuthenticationIsNull() {

        // Arrange — no authentication set
        SecurityContextHolder.clearContext();

        // Act & Assert
        assertThatThrownBy(() -> authService.me())
                .isInstanceOf(NotAuthenticatedException.class)
                .hasMessageContaining("Not authenticated");
    }

    @Test
    @DisplayName("logout should clear SecurityContext and delete refresh token cookie")
    void logout_shouldClearContext_andDeleteCookie() {

        // Arrange
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        Authentication authentication = org.mockito.Mockito.mock(Authentication.class);
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);

        // Act
        authService.logout(httpResponse);

        // Assert
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(httpResponse).addHeader(any(), any());
    }
}
