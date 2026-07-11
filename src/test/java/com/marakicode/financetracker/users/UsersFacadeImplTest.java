package com.marakicode.financetracker.users;

import com.marakicode.financetracker.common.ResourceNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsersFacadeImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UsersFacadeImpl usersFacade;

    @BeforeEach
    void setUp() {
        lenient().when(passwordEncoder.encode(any(String.class)))
                .thenReturn("$2a$10$encodedTempPassword");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private static User sampleUser() {
        var user = new User();
        user.setId(1L);
        user.setFirstName("Alice");
        user.setLastName("Smith");
        user.setEmail("alice@example.com");
        user.setPasswordHash("$2a$10$encodedPassword");
        user.setRole(Role.USER);
        user.setActive(true);
        return user;
    }

    @Test
    @DisplayName("getUserById_validId_returnsUserSummary")
    void getUserById_validId_returnsUserSummary() {
        // Arrange
        var user = sampleUser();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        // Act
        var result = usersFacade.getUserById(1L);

        // Assert
        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.firstName()).isEqualTo("Alice");
        assertThat(result.lastName()).isEqualTo("Smith");
        assertThat(result.email()).isEqualTo("alice@example.com");
        assertThat(result.role()).isEqualTo(Role.USER);
        assertThat(result.active()).isTrue();
        verify(userRepository).findById(1L);
    }

    @Test
    @DisplayName("getUserById_invalidId_throwsResourceNotFoundException")
    void getUserById_invalidId_throwsResourceNotFoundException() {
        // Arrange
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> usersFacade.getUserById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found with id: 999");
    }

    @Test
    @DisplayName("listUsers_noSearch_returnsPagedUsers")
    void listUsers_noSearch_returnsPagedUsers() {
        // Arrange
        var user = sampleUser();
        var page = new PageImpl<>(List.of(user), PageRequest.of(0, 10), 1);
        when(userRepository.findAll(any(Specification.class), eq(PageRequest.of(0, 10))))
                .thenReturn(page);

        // Act
        var result = usersFacade.listUsers(null, PageRequest.of(0, 10));

        // Assert
        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).firstName()).isEqualTo("Alice");
        assertThat(result.page()).isEqualTo(0);
        assertThat(result.size()).isEqualTo(10);
        assertThat(result.totalPages()).isEqualTo(1);
    }

    @Test
    @DisplayName("listUsers_withSearch_returnsFilteredUsers")
    void listUsers_withSearch_returnsFilteredUsers() {
        // Arrange
        var user = sampleUser();
        var page = new PageImpl<>(List.of(user), PageRequest.of(0, 10), 1);
        when(userRepository.findAll(any(Specification.class), eq(PageRequest.of(0, 10))))
                .thenReturn(page);

        // Act
        var result = usersFacade.listUsers("alice", PageRequest.of(0, 10));

        // Assert
        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).email()).isEqualTo("alice@example.com");
    }

    @Test
    @DisplayName("listUsers_emptyPage_returnsEmptyPagedResponse")
    void listUsers_emptyPage_returnsEmptyPagedResponse() {
        // Arrange
        var page = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
        when(userRepository.findAll(any(Specification.class), eq(PageRequest.of(0, 10))))
                .thenReturn(page);

        // Act
        var result = usersFacade.listUsers("nonexistent", PageRequest.of(0, 10));

        // Assert
        assertThat(result.content()).isEmpty();
        assertThat(result.totalPages()).isZero();
    }

    @Test
    @DisplayName("getStatistics_returnsCorrectCounts")
    void getStatistics_returnsCorrectCounts() {
        // Arrange
        when(userRepository.getUserStatistics()).thenReturn(new Object[]{100L, 85L, 15L, 5L, 95L});

        // Act
        var result = usersFacade.getStatistics();

        // Assert
        assertThat(result.totalUsers()).isEqualTo(100L);
        assertThat(result.activeUsers()).isEqualTo(85L);
        assertThat(result.suspendedUsers()).isEqualTo(15L);
        assertThat(result.adminCount()).isEqualTo(5L);
        assertThat(result.regularUserCount()).isEqualTo(95L);
    }

    @Test
    @DisplayName("suspendUser_validId_setsActiveFalse")
    void suspendUser_validId_setsActiveFalse() {
        // Arrange
        var user = sampleUser();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        // Act
        usersFacade.suspendUser(1L);

        // Assert
        var captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().isActive()).isFalse();
    }

    @Test
    @DisplayName("suspendUser_invalidId_throwsResourceNotFoundException")
    void suspendUser_invalidId_throwsResourceNotFoundException() {
        // Arrange
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> usersFacade.suspendUser(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found with id: 999");
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("activateUser_validId_setsActiveTrue")
    void activateUser_validId_setsActiveTrue() {
        // Arrange
        var user = sampleUser();
        user.setActive(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        // Act
        usersFacade.activateUser(1L);

        // Assert
        var captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().isActive()).isTrue();
    }

    @Test
    @DisplayName("resetPassword_validId_returnsTempPassword")
    void resetPassword_validId_returnsTempPassword() {
        // Arrange
        var user = sampleUser();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        // Act
        var tempPassword = usersFacade.resetPassword(1L);

        // Assert
        assertThat(tempPassword).isNotBlank();
        assertThat(tempPassword).hasSize(12);
        verify(passwordEncoder).encode(tempPassword);
        var captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getPasswordHash()).isEqualTo("$2a$10$encodedTempPassword");
    }

    @Test
    @DisplayName("resetPassword_invalidId_throwsResourceNotFoundException")
    void resetPassword_invalidId_throwsResourceNotFoundException() {
        // Arrange
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> usersFacade.resetPassword(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found with id: 999");
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateUserRole_validId_setsRole")
    void updateUserRole_validId_setsRole() {
        // Arrange
        var user = sampleUser();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        // Act
        usersFacade.updateUserRole(1L, Role.ADMIN);

        // Assert
        var captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getRole()).isEqualTo(Role.ADMIN);
    }

    @Test
    @DisplayName("activateUser_invalidId_throwsResourceNotFoundException")
    void activateUser_invalidId_throwsResourceNotFoundException() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> usersFacade.activateUser(999L))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("updateUserRole_invalidId_throwsResourceNotFoundException")
    void updateUserRole_invalidId_throwsResourceNotFoundException() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> usersFacade.updateUserRole(999L, Role.ADMIN))
            .isInstanceOf(ResourceNotFoundException.class);
    }
}
