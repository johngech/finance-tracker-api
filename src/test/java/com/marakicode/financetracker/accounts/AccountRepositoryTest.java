package com.marakicode.financetracker.accounts;

import com.marakicode.financetracker.users.User;
import com.marakicode.financetracker.users.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class AccountRepositoryTest {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private AccountTypeRepository accountTypeRepository;

    @Autowired
    private UserRepository userRepository;

    private AccountTypeEntity checkingType;
    private AccountTypeEntity savingsType;
    private AccountTypeEntity investmentType;
    private User savedUser;

    @BeforeEach
    void setUp() {
        checkingType = accountTypeRepository.save(createTypeEntity("CHECKING"));
        savingsType = accountTypeRepository.save(createTypeEntity("SAVINGS"));
        investmentType = accountTypeRepository.save(createTypeEntity("INVESTMENT"));

        var user = new User();
        user.setFirstName("Alice");
        user.setLastName("Smith");
        user.setEmail("alice@example.com");
        user.setPasswordHash("hashed_password");
        savedUser = userRepository.save(user);
    }

    private AccountTypeEntity createTypeEntity(String name) {
        var type = new AccountTypeEntity();
        type.setName(name);
        return type;
    }

    private AccountTypeEntity resolveType(AccountType type) {
        return switch (type) {
            case CHECKING -> checkingType;
            case SAVINGS -> savingsType;
            case INVESTMENT -> investmentType;
        };
    }

    private Account createTestAccount(String name, AccountType type, BigDecimal balance, String currency) {
        var account = new Account();
        account.setUser(savedUser);
        account.setName(name);
        account.setType(resolveType(type));
        account.setBalance(balance);
        account.setCurrency(currency);
        return account;
    }

    @Test
    @DisplayName("saveAccount_shouldPersistWithGeneratedId - saves account and assigns an ID")
    void saveAccount_shouldPersistWithGeneratedId() {
        // Arrange
        var account = createTestAccount("Checking", AccountType.CHECKING, new BigDecimal("1000.00"), "USD");

        // Act
        var saved = accountRepository.save(account);

        // Assert
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("Checking");
        assertThat(saved.getType().getName()).isEqualTo("CHECKING");
        assertThat(saved.getBalance()).isEqualByComparingTo(new BigDecimal("1000.00"));
        assertThat(saved.getCurrency()).isEqualTo("USD");
        assertThat(saved.getUser().getId()).isEqualTo(savedUser.getId());
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("findByIdAndUserId_shouldReturnAccount_whenOwnedByUser - returns account owned by the user")
    void findByIdAndUserId_shouldReturnAccount_whenOwnedByUser() {
        // Arrange
        var saved = accountRepository.save(
                createTestAccount("Savings", AccountType.SAVINGS, new BigDecimal("5000.00"), "USD"));

        // Act
        var result = accountRepository.findByIdAndUserId(saved.getId(), savedUser.getId());

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(saved.getId());
        assertThat(result.get().getName()).isEqualTo("Savings");
    }

    @Test
    @DisplayName("findByIdAndUserId_shouldReturnEmpty_whenNotOwnedByUser - denies access to another user's account")
    void findByIdAndUserId_shouldReturnEmpty_whenNotOwnedByUser() {
        // Arrange
        var account = accountRepository.save(
                createTestAccount("Investment", AccountType.INVESTMENT, new BigDecimal("10000.00"), "EUR"));

        // Act - search with a different (non-existent) user ID
        var result = accountRepository.findByIdAndUserId(account.getId(), 999L);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findByUserId_shouldReturnAccountsForUser - returns all accounts for a user")
    void findByUserId_shouldReturnAccountsForUser() {
        // Arrange
        accountRepository.save(createTestAccount("Checking", AccountType.CHECKING, new BigDecimal("1000.00"), "USD"));
        accountRepository.save(createTestAccount("Savings", AccountType.SAVINGS, new BigDecimal("5000.00"), "USD"));

        // Act
        var result = accountRepository.findByUserId(savedUser.getId(), PageRequest.of(0, 10));

        // Assert
        assertThat(result.getContent()).hasSize(2);
    }

    @Test
    @DisplayName("findByUserId_shouldReturnEmpty_whenNoAccounts - returns empty for user with no accounts")
    void findByUserId_shouldReturnEmpty_whenNoAccounts() {
        // Act
        var result = accountRepository.findByUserId(savedUser.getId(), PageRequest.of(0, 10));

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("deleteAccount_shouldRemoveFromDatabase - deletes account and verifies removal")
    void deleteAccount_shouldRemoveFromDatabase() {
        // Arrange
        var saved = accountRepository.save(
                createTestAccount("Checking", AccountType.CHECKING, new BigDecimal("1000.00"), "USD"));

        // Act
        accountRepository.delete(saved);

        // Assert
        assertThat(accountRepository.findById(saved.getId())).isEmpty();
    }

    @Test
    @DisplayName("existsByUserIdAndName_shouldReturnTrue_whenAccountExists - returns true for existing account name per user")
    void existsByUserIdAndName_shouldReturnTrue_whenAccountExists() {
        // Arrange
        accountRepository.save(
                createTestAccount("Checking", AccountType.CHECKING, new BigDecimal("1000.00"), "USD"));

        // Act
        var exists = accountRepository.existsByUserIdAndName(savedUser.getId(), "Checking");

        // Assert
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("existsByUserIdAndName_shouldReturnFalse_whenAccountDoesNotExist - returns false for nonexistent name")
    void existsByUserIdAndName_shouldReturnFalse_whenAccountDoesNotExist() {
        // Arrange
        accountRepository.save(
                createTestAccount("Checking", AccountType.CHECKING, new BigDecimal("1000.00"), "USD"));

        // Act
        var exists = accountRepository.existsByUserIdAndName(savedUser.getId(), "Savings");

        // Assert
        assertThat(exists).isFalse();
    }
}
