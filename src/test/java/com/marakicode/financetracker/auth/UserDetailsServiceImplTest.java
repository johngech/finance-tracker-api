package com.marakicode.financetracker.auth;

import com.marakicode.financetracker.users.User;
import com.marakicode.financetracker.users.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTest {

    @Mock
    private UserRepository userRepository;

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
        when(userRepository.findByEmailIgnoreCase("alice@example.com")).thenReturn(Optional.of(user));

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
        when(userRepository.findByEmailIgnoreCase("nonexistent@example.com"))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("nonexistent@example.com"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("User not found with email: nonexistent@example.com");
    }

    @Test
    @DisplayName("loadUserByUsername should return disabled UserDetails when user is inactive")
    void loadUserByUsername_shouldReturnDisabledUserDetails_whenUserInactive() {

        // Arrange
        var user = sampleUser();
        user.setActive(false);
        when(userRepository.findByEmailIgnoreCase("alice@example.com")).thenReturn(Optional.of(user));

        // Act
        var userDetails = userDetailsService.loadUserByUsername("alice@example.com");

        // Assert
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.isEnabled()).isFalse();
    }
}
