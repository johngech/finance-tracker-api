package com.marakicode.financetracker.auth;

import com.marakicode.financetracker.common.ResourceNotFoundException;
import com.marakicode.financetracker.users.User;
import com.marakicode.financetracker.users.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserDetailsServiceImpl userDetailsService;

    private static User sampleUser() {
        User user = new User();
        user.setFirstName("Alice");
        user.setLastName("Smith");
        user.setEmail("alice@example.com");
        user.setPasswordHash("$2a$10$encodedPassword");
        return user;
    }

    @Test
    @DisplayName("loadUserByUsername should return UserDetails with ROLE_USER authority when email exists")
    void loadUserByUsername_shouldReturnUserDetails_whenEmailExists() {

        // Arrange
        var user = sampleUser();
        when(userService.findByEmail("alice@example.com")).thenReturn(user);

        // Act
        var userDetails = userDetailsService.loadUserByUsername("alice@example.com");

        // Assert
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo("alice@example.com");
        assertThat(userDetails.getPassword()).isEqualTo("$2a$10$encodedPassword");
        assertThat(userDetails.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_USER");
    }

    @Test
    @DisplayName("loadUserByUsername should throw UsernameNotFoundException when email not found")
    void loadUserByUsername_shouldThrow_whenEmailNotFound() {

        // Arrange
        when(userService.findByEmail("nonexistent@example.com"))
                .thenThrow(new ResourceNotFoundException("User not found with email: nonexistent@example.com"));

        // Act & Assert
        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("nonexistent@example.com"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("User not found with email: nonexistent@example.com");
    }
}
