package com.marakicode.financetracker.common;

import com.marakicode.financetracker.users.User;
import com.marakicode.financetracker.users.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Security Utils Tests")
class SecurityUtilsTest {

    @Mock
    private UserService userService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("getCurrentUser_validAuth_returnsUser - returns the user matching the authenticated email")
    void getCurrentUser_validAuth_returnsUser() {
        // Arrange
        User user = new User();
        user.setId(1L);
        user.setEmail("alice@example.com");

        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("alice@example.com");
        SecurityContextHolder.getContext().setAuthentication(auth);

        when(userService.findByEmail("alice@example.com")).thenReturn(user);

        // Act
        var result = SecurityUtils.getCurrentUser(userService);

        // Assert
        assertThat(result).isEqualTo(user);
        verify(userService).findByEmail("alice@example.com");
    }

    @Test
    @DisplayName("getCurrentUser_nullAuth_throwsAccessDeniedException - no authentication in context")
    void getCurrentUser_nullAuth_throwsAccessDeniedException() {
        // Arrange — no authentication set
        SecurityContextHolder.clearContext();

        // Act & Assert
        assertThatThrownBy(() -> SecurityUtils.getCurrentUser(userService))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Not authenticated");
        verify(userService, never()).findByEmail(null);
    }

    @Test
    @DisplayName("getCurrentUser_blankName_throwsAccessDeniedException - authentication with blank name")
    void getCurrentUser_blankName_throwsAccessDeniedException() {
        // Arrange
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("   ");
        SecurityContextHolder.getContext().setAuthentication(auth);

        // Act & Assert
        assertThatThrownBy(() -> SecurityUtils.getCurrentUser(userService))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Not authenticated");
    }

    @Test
    @DisplayName("getCurrentUser_nullName_throwsAccessDeniedException - authentication with null name")
    void getCurrentUser_nullName_throwsAccessDeniedException() {
        // Arrange
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn(null);
        SecurityContextHolder.getContext().setAuthentication(auth);

        // Act & Assert
        assertThatThrownBy(() -> SecurityUtils.getCurrentUser(userService))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Not authenticated");
    }

    @Test
    @DisplayName("getCurrentUser_realToken_returnsUser - works with actual UsernamePasswordAuthenticationToken")
    void getCurrentUser_realToken_returnsUser() {
        // Arrange
        User user = new User();
        user.setId(1L);
        user.setEmail("bob@example.com");

        Authentication auth = new UsernamePasswordAuthenticationToken("bob@example.com", null);
        SecurityContextHolder.getContext().setAuthentication(auth);

        when(userService.findByEmail("bob@example.com")).thenReturn(user);

        // Act
        var result = SecurityUtils.getCurrentUser(userService);

        // Assert
        assertThat(result).isEqualTo(user);
    }
}
