package com.marakicode.financetracker.auth;

import com.marakicode.financetracker.users.User;
import com.marakicode.financetracker.users.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private UserService userService;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    private static final String VALID_TOKEN = "valid.jwt.token";
    private static final String BEARER_PREFIX = "Bearer ";

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        SecurityContextHolder.clearContext();
    }

    private User testUser() {
        User user = new User();
        user.setId(1L);
        user.setEmail("alice@example.com");
        user.setPasswordHash("encodedPassword");
        return user;
    }

    private Jwt validJwt() {
        io.jsonwebtoken.Claims claims = io.jsonwebtoken.Jwts.claims()
                .subject("1")
                .add("email", "alice@example.com")
                .add("role", "USER")
                .add("type", "access")
                .expiration(new java.util.Date(System.currentTimeMillis() + 900000))
                .build();
        javax.crypto.SecretKey key = io.jsonwebtoken.security.Keys.hmacShaKeyFor(
                "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970"
                        .getBytes(StandardCharsets.UTF_8));
        return new Jwt(key, claims);
    }

    @Test
    @DisplayName("doFilterInternal should set authentication when valid JWT is provided")
    void doFilterInternal_shouldSetAuthentication_whenValidJwt() throws ServletException, IOException {

        // Arrange
        request.addHeader("Authorization", BEARER_PREFIX + VALID_TOKEN);
        Jwt jwt = validJwt();

        when(jwtService.parseToken(VALID_TOKEN)).thenReturn(Optional.of(jwt));
        when(userService.findById(1L)).thenReturn(testUser());

        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getPrincipal()).isInstanceOf(UserIdPrincipal.class);
        var principal = (UserIdPrincipal) authentication.getPrincipal();
        assertThat(principal.id()).isEqualTo(1L);
        assertThat(principal.email()).isEqualTo("alice@example.com");
        assertThat(authentication.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_USER");
    }

    @Test
    @DisplayName("doFilterInternal should not set authentication when no Authorization header is present")
    void doFilterInternal_shouldNotSetAuthentication_whenNoAuthHeader() throws ServletException, IOException {

        // Arrange — no Authorization header

        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNull();
    }

    @Test
    @DisplayName("doFilterInternal should not set authentication when JWT is invalid")
    void doFilterInternal_shouldNotSetAuthentication_whenInvalidJwt() throws ServletException, IOException {

        // Arrange
        request.addHeader("Authorization", BEARER_PREFIX + "invalid.token");
        when(jwtService.parseToken("invalid.token")).thenReturn(Optional.empty());

        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNull();
    }

    @Test
    @DisplayName("doFilterInternal should not set authentication when JWT is expired")
    void doFilterInternal_shouldNotSetAuthentication_whenExpiredJwt() throws ServletException, IOException {

        // Arrange
        request.addHeader("Authorization", BEARER_PREFIX + VALID_TOKEN);
        io.jsonwebtoken.Claims expiredClaims = io.jsonwebtoken.Jwts.claims()
                .subject("1")
                .add("email", "alice@example.com")
                .add("role", "USER")
                .add("type", "access")
                .expiration(new java.util.Date(System.currentTimeMillis() - 1000))
                .build();
        javax.crypto.SecretKey key = io.jsonwebtoken.security.Keys.hmacShaKeyFor(
                "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970"
                        .getBytes(StandardCharsets.UTF_8));
        Jwt expiredJwt = new Jwt(key, expiredClaims);

        when(jwtService.parseToken(VALID_TOKEN)).thenReturn(Optional.of(expiredJwt));

        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNull();
    }

    @Test
    @DisplayName("doFilterInternal should always call filterChain.doFilter in all cases")
    void doFilterInternal_shouldAlwaysCallFilterChain() throws ServletException, IOException {

        // Act — no Authorization header
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("doFilterInternal should not set authentication when user is inactive")
    void doFilterInternal_shouldNotSetAuthentication_whenUserInactive() throws ServletException, IOException {

        // Arrange
        request.addHeader("Authorization", BEARER_PREFIX + VALID_TOKEN);
        Jwt jwt = validJwt();
        User inactiveUser = testUser();
        inactiveUser.setActive(false);

        when(jwtService.parseToken(VALID_TOKEN)).thenReturn(Optional.of(jwt));
        when(userService.findById(1L)).thenReturn(inactiveUser);

        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNull();
    }
}
