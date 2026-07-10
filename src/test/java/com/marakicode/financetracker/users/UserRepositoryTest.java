package com.marakicode.financetracker.users;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    private User createTestUser(String firstName, String lastName, String email) {
        var user = new User();
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(email);
        user.setPasswordHash("hashed_password");
        return user;
    }

    @Test
    @DisplayName("saveUser_shouldPersistWithGeneratedId - saves user and assigns an ID")
    void saveUser_shouldPersistWithGeneratedId() {

        // Arrange
        var user = createTestUser("Alice", "Smith", "alice@example.com");

        // Act
        var saved = userRepository.save(user);

        // Assert
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getFirstName()).isEqualTo("Alice");
        assertThat(saved.getLastName()).isEqualTo("Smith");
        assertThat(saved.getEmail()).isEqualTo("alice@example.com");
        assertThat(saved.getPasswordHash()).isEqualTo("hashed_password");
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("findByEmail_shouldReturnUser - returns matching user when email exists")
    void findByEmail_shouldReturnUser() {

        // Arrange
        var saved = userRepository.save(createTestUser("Bob", "Jones", "bob@example.com"));

        // Act
        var result = userRepository.findByEmailIgnoreCase("bob@example.com");

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(saved.getId());
        assertThat(result.get().getFirstName()).isEqualTo("Bob");
        assertThat(result.get().getEmail()).isEqualTo("bob@example.com");
    }

    @Test
    @DisplayName("findByEmail_shouldReturnEmptyWhenNotFound - returns empty for unknown email")
    void findByEmail_shouldReturnEmptyWhenNotFound() {

        // Arrange & Act
        var result = userRepository.findByEmailIgnoreCase("nonexistent@example.com");

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("existsByEmail_shouldReturnTrueWhenExists - returns true for existing email")
    void existsByEmail_shouldReturnTrueWhenExists() {

        // Arrange
        userRepository.save(createTestUser("Charlie", "Brown", "charlie@example.com"));

        // Act
        var exists = userRepository.existsByEmailIgnoreCase("charlie@example.com");

        // Assert
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("existsByEmail_shouldReturnFalseWhenNotExists - returns false for unknown email")
    void existsByEmail_shouldReturnFalseWhenNotExists() {

        // Arrange & Act
        var exists = userRepository.existsByEmailIgnoreCase("nobody@example.com");

        // Assert
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("uniqueEmail_shouldFailOnDuplicate - throws on duplicate email")
    void uniqueEmail_shouldFailOnDuplicate() {

        // Arrange
        userRepository.save(createTestUser("Frank", "Lee", "frank@example.com"));
        var duplicate = createTestUser("Different", "Person", "frank@example.com");

        // Act & Assert
        assertThatThrownBy(() -> userRepository.save(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
