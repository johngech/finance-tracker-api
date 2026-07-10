package com.marakicode.financetracker.auth;

import com.marakicode.financetracker.users.User;
import com.marakicode.financetracker.users.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    private static final String TEST_SECRET =
            "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";

    @Mock
    private JwtConfig jwtConfig;

    @InjectMocks
    private JwtService jwtService;

    private void stubSecret() {
        when(jwtConfig.getSecretKey())
                .thenReturn(io.jsonwebtoken.security.Keys.hmacShaKeyFor(
                        TEST_SECRET.getBytes(StandardCharsets.UTF_8)));
    }

    private User testUser() {
        User user = new User();
        user.setId(1L);
        user.setEmail("alice@example.com");
        user.setPasswordHash("encodedPassword");
        return user;
    }

    @Test
    @DisplayName("generateAccessToken should return non-null Jwt when given valid User")
    void generateAccessToken_shouldReturnJwt_whenValidUser() {
        // Arrange
        stubSecret();
        when(jwtConfig.getAccessTokenExpiration()).thenReturn(900000L);

        // Act
        Jwt jwt = jwtService.generateAccessToken(testUser());

        // Assert
        assertThat(jwt).isNotNull();
        assertThat(jwt.toString()).isNotNull().isNotBlank();
    }

    @Test
    @DisplayName("generateRefreshToken should return non-null Jwt when given valid User")
    void generateRefreshToken_shouldReturnJwt_whenValidUser() {
        // Arrange
        stubSecret();
        when(jwtConfig.getRefreshTokenExpiration()).thenReturn(604800000L);

        // Act
        Jwt jwt = jwtService.generateRefreshToken(testUser());

        // Assert
        assertThat(jwt).isNotNull();
        assertThat(jwt.toString()).isNotNull().isNotBlank();
    }

    @Test
    @DisplayName("parseToken should return Optional Jwt when given a valid token")
    void parseToken_shouldReturnJwt_whenValidToken() {
        // Arrange
        stubSecret();
        when(jwtConfig.getAccessTokenExpiration()).thenReturn(900000L);
        Jwt generated = jwtService.generateAccessToken(testUser());

        // Act
        Optional<Jwt> parsed = jwtService.parseToken(generated.toString());

        // Assert
        assertThat(parsed).isPresent();
        assertThat(parsed.get().getUserId()).isEqualTo(1L);
        assertThat(parsed.get().isExpired()).isFalse();
    }

    @Test
    @DisplayName("parseToken should return empty Optional when given an invalid token")
    void parseToken_shouldReturnEmpty_whenInvalidToken() {
        // Arrange
        stubSecret();

        // Act
        Optional<Jwt> result = jwtService.parseToken("invalid.token.string");

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Jwt.isExpired should return false for a valid token")
    void isExpired_shouldReturnFalse_whenValidToken() {
        // Arrange
        stubSecret();
        when(jwtConfig.getAccessTokenExpiration()).thenReturn(900000L);
        Jwt jwt = jwtService.generateAccessToken(testUser());

        // Act & Assert
        assertThat(jwt.isExpired()).isFalse();
    }

    @Test
    @DisplayName("Jwt.isExpired should return true for an expired token")
    void isExpired_shouldReturnTrue_whenExpiredToken() {
        // Arrange — 0ms expiration so the token is immediately expired
        stubSecret();
        when(jwtConfig.getAccessTokenExpiration()).thenReturn(0L);
        Jwt jwt = jwtService.generateAccessToken(testUser());

        // Act & Assert
        assertThat(jwt.isExpired()).isTrue();
    }

    @Test
    @DisplayName("Jwt.getUserId should return user ID from the claims")
    void getUserId_shouldReturnUserId() {
        // Arrange
        stubSecret();
        when(jwtConfig.getAccessTokenExpiration()).thenReturn(900000L);
        Jwt jwt = jwtService.generateAccessToken(testUser());

        // Act & Assert
        assertThat(jwt.getUserId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Jwt.toString should return the compact token string")
    void toString_shouldReturnCompactTokenString() {
        // Arrange
        stubSecret();
        when(jwtConfig.getAccessTokenExpiration()).thenReturn(900000L);
        Jwt jwt = jwtService.generateAccessToken(testUser());

        // Act
        String tokenString = jwt.toString();

        // Assert
        assertThat(tokenString).isNotNull().isNotBlank();
        assertThat(tokenString.split("\\.")).hasSize(3);
    }

    @Test
    @DisplayName("generateAccessToken should include role claim in token")
    void generateAccessToken_shouldIncludeRoleClaim() {
        // Arrange
        stubSecret();
        when(jwtConfig.getAccessTokenExpiration()).thenReturn(900000L);
        Jwt jwt = jwtService.generateAccessToken(testUser());

        // Act & Assert
        assertThat(jwt.getRole()).isEqualTo("USER");
    }

    @Test
    @DisplayName("generateAccessToken should include type claim as 'access'")
    void generateAccessToken_shouldIncludeTypeAccessClaim() {
        // Arrange
        stubSecret();
        when(jwtConfig.getAccessTokenExpiration()).thenReturn(900000L);
        Jwt jwt = jwtService.generateAccessToken(testUser());

        // Act & Assert
        assertThat(jwt.getType()).isEqualTo("access");
        assertThat(jwt.isRefreshToken()).isFalse();
    }

    @Test
    @DisplayName("generateRefreshToken should include type claim as 'refresh'")
    void generateRefreshToken_shouldIncludeTypeRefreshClaim() {
        // Arrange
        stubSecret();
        when(jwtConfig.getRefreshTokenExpiration()).thenReturn(604800000L);
        Jwt jwt = jwtService.generateRefreshToken(testUser());

        // Act & Assert
        assertThat(jwt.getType()).isEqualTo("refresh");
        assertThat(jwt.isRefreshToken()).isTrue();
    }

    @Test
    @DisplayName("getAccessTokenExpiration should delegate to JwtConfig")
    void getAccessTokenExpiration_shouldDelegateToJwtConfig() {
        when(jwtConfig.getAccessTokenExpiration()).thenReturn(900000L);
        assertThat(jwtService.getAccessTokenExpiration()).isEqualTo(900000L);
    }

    @Test
    @DisplayName("getRefreshTokenExpiration should delegate to JwtConfig")
    void getRefreshTokenExpiration_shouldDelegateToJwtConfig() {
        when(jwtConfig.getRefreshTokenExpiration()).thenReturn(604800000L);
        assertThat(jwtService.getRefreshTokenExpiration()).isEqualTo(604800000L);
    }
}
