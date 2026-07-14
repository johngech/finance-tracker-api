package com.marakicode.financetracker.users;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminInitializerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private Environment environment;

    @InjectMocks
    private AdminInitializer adminInitializer;

    @Test
    @DisplayName("run creates admin when no admin exists — seeds first admin from env vars")
    void run_createsAdmin_whenNoAdminExists() {
        // Arrange
        when(userRepository.countByRole(Role.ADMIN)).thenReturn(0L);
        when(environment.getProperty("ADMIN_EMAIL")).thenReturn("admin@example.com");
        when(environment.getProperty("ADMIN_PASSWORD")).thenReturn("Secret123!");
        when(environment.getProperty("ADMIN_FIRST_NAME")).thenReturn("Admin");
        when(environment.getProperty("ADMIN_LAST_NAME")).thenReturn("User");
        when(userRepository.existsByEmailIgnoreCase("admin@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Secret123!")).thenReturn("$2a$10$encodedPassword");

        // Act
        adminInitializer.run();

        // Assert
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getEmail()).isEqualTo("admin@example.com");
        assertThat(saved.getFirstName()).isEqualTo("Admin");
        assertThat(saved.getLastName()).isEqualTo("User");
        assertThat(saved.getRole()).isEqualTo(Role.ADMIN);
        assertThat(saved.getPasswordHash()).isEqualTo("$2a$10$encodedPassword");
        assertThat(saved.isActive()).isTrue();
    }

    @Test
    @DisplayName("run skips when admin already exists — no save attempted")
    void run_skips_whenAdminAlreadyExists() {
        // Arrange
        when(userRepository.countByRole(Role.ADMIN)).thenReturn(1L);

        // Act
        adminInitializer.run();

        // Assert
        verify(userRepository, never()).save(any());
        verify(environment, never()).getProperty(anyString());
    }

    @Test
    @DisplayName("run skips when env vars are missing — no save attempted")
    void run_skips_whenEnvVarsMissing() {
        // Arrange
        when(userRepository.countByRole(Role.ADMIN)).thenReturn(0L);
        when(environment.getProperty("ADMIN_EMAIL")).thenReturn(null);
        when(environment.getProperty("ADMIN_PASSWORD")).thenReturn(null);
        when(environment.getProperty("ADMIN_FIRST_NAME")).thenReturn(null);
        when(environment.getProperty("ADMIN_LAST_NAME")).thenReturn(null);

        // Act
        adminInitializer.run();

        // Assert
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("run skips when only some env vars are present — blank last name rejected")
    void run_skips_whenPartialEnvVars() {
        // Arrange
        when(userRepository.countByRole(Role.ADMIN)).thenReturn(0L);
        when(environment.getProperty("ADMIN_EMAIL")).thenReturn("admin@example.com");
        when(environment.getProperty("ADMIN_PASSWORD")).thenReturn("Secret123!");
        when(environment.getProperty("ADMIN_FIRST_NAME")).thenReturn("Admin");
        when(environment.getProperty("ADMIN_LAST_NAME")).thenReturn("");

        // Act
        adminInitializer.run();

        // Assert
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("run skips when email already registered — concurrent seed protection")
    void run_skips_whenEmailAlreadyRegistered() {
        // Arrange
        when(userRepository.countByRole(Role.ADMIN)).thenReturn(0L);
        when(environment.getProperty("ADMIN_EMAIL")).thenReturn("admin@example.com");
        when(environment.getProperty("ADMIN_PASSWORD")).thenReturn("Secret123!");
        when(environment.getProperty("ADMIN_FIRST_NAME")).thenReturn("Admin");
        when(environment.getProperty("ADMIN_LAST_NAME")).thenReturn("User");
        when(userRepository.existsByEmailIgnoreCase("admin@example.com")).thenReturn(true);

        // Act
        adminInitializer.run();

        // Assert
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("run normalizes email to lowercase — consistent storage format")
    void run_normalizesEmailToLowerCase() {
        // Arrange
        when(userRepository.countByRole(Role.ADMIN)).thenReturn(0L);
        when(environment.getProperty("ADMIN_EMAIL")).thenReturn("ADMIN@EXAMPLE.COM");
        when(environment.getProperty("ADMIN_PASSWORD")).thenReturn("Secret123!");
        when(environment.getProperty("ADMIN_FIRST_NAME")).thenReturn("Admin");
        when(environment.getProperty("ADMIN_LAST_NAME")).thenReturn("User");
        when(userRepository.existsByEmailIgnoreCase("admin@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Secret123!")).thenReturn("$2a$10$encodedPassword");

        // Act
        adminInitializer.run();

        // Assert
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getEmail()).isEqualTo("admin@example.com");
    }

    @Test
    @DisplayName("run catches DataIntegrityViolationException — graceful handling under concurrent init")
    void run_catchesDataIntegrityViolationException() {
        // Arrange
        when(userRepository.countByRole(Role.ADMIN)).thenReturn(0L);
        when(environment.getProperty("ADMIN_EMAIL")).thenReturn("admin@example.com");
        when(environment.getProperty("ADMIN_PASSWORD")).thenReturn("Secret123!");
        when(environment.getProperty("ADMIN_FIRST_NAME")).thenReturn("Admin");
        when(environment.getProperty("ADMIN_LAST_NAME")).thenReturn("User");
        when(userRepository.existsByEmailIgnoreCase("admin@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Secret123!")).thenReturn("$2a$10$encodedPassword");
        when(userRepository.save(any(User.class)))
                .thenThrow(new DataIntegrityViolationException("Duplicate key"));

        // Act — should NOT throw
        adminInitializer.run();

        // Assert — save was attempted but exception was caught
        verify(userRepository).save(any(User.class));
    }
}
