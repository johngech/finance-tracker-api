package com.marakicode.financetracker.auth;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthCurrentUserProvider")
class AuthCurrentUserProviderTest {

    @InjectMocks
    private AuthCurrentUserProvider authCurrentUserProvider;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("given valid UserIdPrincipal in SecurityContext, returns principal id")
    void getCurrentUserId_validPrincipal_returnsId() {
        // Arrange
        var principal = new UserIdPrincipal(1L, "test@example.com");
        var auth = new UsernamePasswordAuthenticationToken(principal, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);

        // Act
        Long result = authCurrentUserProvider.getCurrentUserId();

        // Assert
        assertThat(result).isEqualTo(1L);
    }

    @Test
    @DisplayName("given null authentication in SecurityContext, throws NotAuthenticatedException")
    void getCurrentUserId_nullAuth_throwsNotAuthenticatedException() {
        // Arrange — SecurityContext is empty (no authentication set)

        // Act & Assert
        assertThatThrownBy(() -> authCurrentUserProvider.getCurrentUserId())
                .isInstanceOf(NotAuthenticatedException.class)
                .hasMessage("Not authenticated");
    }

    @Test
    @DisplayName("given authentication with wrong principal type, throws NotAuthenticatedException")
    void getCurrentUserId_wrongPrincipalType_throwsNotAuthenticatedException() {
        // Arrange — set a String principal instead of UserIdPrincipal
        var auth = new UsernamePasswordAuthenticationToken("test@example.com", null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);

        // Act & Assert
        assertThatThrownBy(() -> authCurrentUserProvider.getCurrentUserId())
                .isInstanceOf(NotAuthenticatedException.class)
                .hasMessage("Not authenticated");
    }
}
