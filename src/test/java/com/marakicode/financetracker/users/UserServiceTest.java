package com.marakicode.financetracker.users;

import com.marakicode.financetracker.common.DuplicateResourceException;
import com.marakicode.financetracker.common.ResourceNotFoundException;
import com.marakicode.financetracker.users.exceptions.PasswordMismatchException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
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
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserService userService;

    private static User sampleUser() {
        User user = new User();
        user.setId(1L);
        user.setFirstName("Alice");
        user.setLastName("Smith");
        user.setEmail("alice@example.com");
        user.setPasswordHash("$2a$10$encodedPassword");
        return user;
    }

    private static UserDto sampleUserDto() {
        return new UserDto(1L, "Alice", "Smith", "alice@example.com", LocalDateTime.of(2025, 1, 15, 10, 30));
    }

    @Test
    @DisplayName("createUser_shouldReturnDto_whenValidRequest - creates user with encoded password")
    void createUser_shouldReturnDto_whenValidRequest() {
        // Arrange
        var request = new UserCreateRequest("Alice", "Smith", "alice@example.com", "Secret123!");
        var user = sampleUser();
        var dto = sampleUserDto();

        when(userRepository.existsByEmailIgnoreCase("alice@example.com")).thenReturn(false);
        when(userMapper.toEntity(request)).thenReturn(user);
        when(passwordEncoder.encode("Secret123!")).thenReturn("$2a$10$encodedPassword");
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toDto(user)).thenReturn(dto);

        // Act
        var result = userService.createUser(request);

        // Assert
        assertThat(result).isEqualTo(dto);
        verify(userRepository).existsByEmailIgnoreCase("alice@example.com");
        verify(passwordEncoder).encode("Secret123!");
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("createUser_shouldThrow_whenEmailExists - rejects duplicate email")
    void createUser_shouldThrow_whenEmailExists() {
        // Arrange
        var request = new UserCreateRequest("Alice", "Smith", "alice@example.com", "Secret123!");
        when(userRepository.existsByEmailIgnoreCase("alice@example.com")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Email already registered");
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("createUser_shouldNormalizeEmailToLowerCase - stores email in lowercase")
    void createUser_shouldNormalizeEmailToLowerCase() {
        // Arrange
        var request = new UserCreateRequest("Alice", "Smith", "ALICE@EXAMPLE.COM", "Secret123!");
        var user = sampleUser();
        user.setEmail("alice@example.com");
        var dto = sampleUserDto();

        when(userRepository.existsByEmailIgnoreCase("alice@example.com")).thenReturn(false);
        when(userMapper.toEntity(request)).thenReturn(user);
        when(passwordEncoder.encode("Secret123!")).thenReturn("$2a$10$encodedPassword");
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toDto(user)).thenReturn(dto);

        // Act
        var result = userService.createUser(request);

        // Assert
        assertThat(result).isEqualTo(dto);
        verify(userRepository).existsByEmailIgnoreCase("alice@example.com");
        assertThat(user.getEmail()).isEqualTo("alice@example.com");
    }

    @Test
    @DisplayName("createUser_shouldThrow_whenEmailExistsCaseInsensitive - rejects duplicate regardless of case")
    void createUser_shouldThrow_whenEmailExistsCaseInsensitive() {
        // Arrange
        var request = new UserCreateRequest("Alice", "Smith", "Alice@Example.com", "Secret123!");
        when(userRepository.existsByEmailIgnoreCase("alice@example.com")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Email already registered");
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("getUserById_shouldReturnDto_whenUserExists - fetches user by id")
    void getUserById_shouldReturnDto_whenUserExists() {
        // Arrange
        var user = sampleUser();
        var dto = sampleUserDto();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userMapper.toDto(user)).thenReturn(dto);

        // Act
        var result = userService.getUserById(1L);

        // Assert
        assertThat(result).isEqualTo(dto);
    }

    @Test
    @DisplayName("getUserById_shouldThrow_whenUserNotFound - throws for nonexistent id")
    void getUserById_shouldThrow_whenUserNotFound() {
        // Arrange
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userService.getUserById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    @DisplayName("getAllUsers_shouldReturnPagedResponse - returns paginated results")
    void getAllUsers_shouldReturnPagedResponse() {
        // Arrange
        var user = sampleUser();
        var dto = sampleUserDto();
        Page<User> page = new PageImpl<>(List.of(user), PageRequest.of(0, 10), 1);
        when(userRepository.findAll(PageRequest.of(0, 10))).thenReturn(page);
        when(userMapper.toDto(user)).thenReturn(dto);

        // Act
        var result = userService.getAllUsers(PageRequest.of(0, 10));

        // Assert
        assertThat(result.content()).hasSize(1);
        assertThat(result.page()).isEqualTo(0);
        assertThat(result.size()).isEqualTo(10);
        assertThat(result.totalPages()).isEqualTo(1);
    }

    @Test
    @DisplayName("updateUser_shouldReturnDto_whenValidRequest - updates user fields")
    void updateUser_shouldReturnDto_whenValidRequest() {
        // Arrange
        var request = new UserUpdateRequest("Alice", "Johnson");
        var user = sampleUser();
        var updatedDto = new UserDto(1L, "Alice", "Johnson", "alice@example.com", LocalDateTime.now());
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toDto(user)).thenReturn(updatedDto);

        // Act
        var result = userService.updateUser(1L, request);

        // Assert
        assertThat(result).isEqualTo(updatedDto);
        verify(userMapper).updateEntity(request, user);
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("updatePassword_shouldSucceed_whenOldPasswordMatches - encodes and saves new password")
    void updatePassword_shouldSucceed_whenOldPasswordMatches() {
        // Arrange
        var request = new PasswordUpdateRequest("OldPass123!", "NewPass456!");
        var user = sampleUser();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("OldPass123!", "$2a$10$encodedPassword")).thenReturn(true);
        when(passwordEncoder.encode("NewPass456!")).thenReturn("$2a$10$newEncoded");

        // Act
        userService.updatePassword(1L, request);

        // Assert
        verify(passwordEncoder).matches("OldPass123!", "$2a$10$encodedPassword");
        verify(passwordEncoder).encode("NewPass456!");
        assertThat(user.getPasswordHash()).isEqualTo("$2a$10$newEncoded");
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("updatePassword_shouldThrow_whenOldPasswordIncorrect - rejects wrong password")
    void updatePassword_shouldThrow_whenOldPasswordIncorrect() {
        // Arrange
        var request = new PasswordUpdateRequest("WrongPass!", "NewPass456!");
        var user = sampleUser();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("WrongPass!", "$2a$10$encodedPassword")).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> userService.updatePassword(1L, request))
                .isInstanceOf(PasswordMismatchException.class)
                .hasMessageContaining("Current password is incorrect");
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("deleteUser_shouldDelete_whenUserExists - removes user from repository")
    void deleteUser_shouldDelete_whenUserExists() {
        // Arrange
        var user = sampleUser();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        // Act
        userService.deleteUser(1L);

        // Assert
        verify(userRepository).delete(user);
    }

    @Test
    @DisplayName("deleteUser_shouldThrow_whenUserNotFound - throws for nonexistent id")
    void deleteUser_shouldThrow_whenUserNotFound() {
        // Arrange
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userService.deleteUser(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");
        verify(userRepository, never()).delete(any());
    }
}
