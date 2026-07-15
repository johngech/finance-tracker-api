package com.marakicode.financetracker.transactions;

import com.marakicode.financetracker.accounts.*;
import com.marakicode.financetracker.users.User;
import com.marakicode.financetracker.users.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@DisplayName("Transaction Repository Tests")
class TransactionRepositoryTest {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountTypeRepository accountTypeRepository;

    @Autowired
    private TransactionTypeRepository transactionTypeRepository;

    @Autowired
    private TransactionCategoryRepository transactionCategoryRepository;

    @Autowired
    private EntityManager entityManager;

    private User savedUser;
    private Account savedAccount;
    private TransactionTypeEntity incomeType;
    private TransactionTypeEntity expenseType;

    @BeforeEach
    void setUp() {
        var user = new User();
        user.setFirstName("Test");
        user.setLastName("User");
        user.setEmail("testrepo@example.com");
        user.setPasswordHash("hashed_password");
        savedUser = userRepository.save(user);

        AccountTypeEntity accountType = accountTypeRepository.findByName(AccountType.CHECKING.name())
                .orElseGet(() -> accountTypeRepository.save(createAccountTypeEntity(AccountType.CHECKING.name())));

        var account = new Account();
        account.setName("Test Account");
        account.setType(accountType);
        account.setBalance(new BigDecimal("1000.00"));
        account.setCurrency("USD");
        account.setUser(savedUser);
        savedAccount = accountRepository.save(account);

        incomeType = transactionTypeRepository.findByName("INCOME")
                .orElseGet(() -> {
                    var t = new TransactionTypeEntity();
                    t.setName("INCOME");
                    return transactionTypeRepository.save(t);
                });

        expenseType = transactionTypeRepository.findByName("EXPENSE")
                .orElseGet(() -> {
                    var t = new TransactionTypeEntity();
                    t.setName("EXPENSE");
                    return transactionTypeRepository.save(t);
                });
    }

    private AccountTypeEntity createAccountTypeEntity(String name) {
        var type = new AccountTypeEntity();
        type.setName(name);
        return type;
    }

    private TransactionCategoryEntity findOrCreateCategory(String name) {
        return transactionCategoryRepository.findByName(name)
                .orElseGet(() -> {
                    var entity = new TransactionCategoryEntity();
                    entity.setName(name);
                    return transactionCategoryRepository.save(entity);
                });
    }

    /**
     * Overrides the transaction_date via native SQL to bypass @CreationTimestamp.
     * Used in tests that need specific dates for date-range filtering.
     */
    private void overrideTransactionDate(Transaction transaction, LocalDate date) {
        entityManager.createNativeQuery(
                "UPDATE transactions SET transaction_date = ? WHERE id = ?")
                .setParameter(1, date)
                .setParameter(2, transaction.getId())
                .executeUpdate();
        entityManager.flush();
        entityManager.clear();
    }

    private Transaction createTransaction(Account account, TransactionType type, BigDecimal amount,
                                          String description, String category) {
        var transaction = new Transaction();
        transaction.setAccount(account);
        transaction.setType(type == TransactionType.INCOME ? incomeType : expenseType);
        transaction.setAmount(amount);
        transaction.setDescription(description);
        // transactionDate will be set automatically by @CreationTimestamp annotation
        if (category != null) {
            transaction.setCategory(findOrCreateCategory(category));
        }
        return transaction;
    }

    @Test
    @DisplayName("saveTransaction_shouldPersistWithGeneratedId - saves transaction and assigns an ID with correct fields")
    void saveTransaction_persistsWithGeneratedId() {
        // Arrange
        var transaction = createTransaction(savedAccount, TransactionType.INCOME, new BigDecimal("500.00"),
                "Salary",  "SALARY");

        // Act
        var saved = transactionRepository.save(transaction);

        // Assert
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getAccount().getId()).isEqualTo(savedAccount.getId());
        assertThat(saved.getType().getName()).isEqualTo("INCOME");
        assertThat(saved.getAmount()).isEqualByComparingTo(new BigDecimal("500.00"));
        assertThat(saved.getDescription()).isEqualTo("Salary");
        // transactionDate is now set automatically by @CreationTimestamp
        assertThat(saved.getCategory().getName()).isEqualTo("SALARY");
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("findByIdAndUserId_shouldReturnTransaction_whenOwnedByUser - returns transaction owned by the user")
    void findByIdAndUserId_whenOwned_returnsTransaction() {
        // Arrange
        var saved = transactionRepository.save(
                createTransaction(savedAccount, TransactionType.EXPENSE, new BigDecimal("50.00"),
                        "Groceries",  "FOOD"));

        // Act
        var result = transactionRepository.findByIdAndUserId(saved.getId(), savedUser.getId());

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(saved.getId());
        assertThat(result.get().getDescription()).isEqualTo("Groceries");
    }

    @Test
    @DisplayName("findByIdAndUserId_shouldReturnEmpty_whenNotOwnedByUser - denies access to another user's transaction")
    void findByIdAndUserId_whenNotOwned_returnsEmpty() {
        // Arrange
        var saved = transactionRepository.save(
                createTransaction(savedAccount, TransactionType.INCOME, new BigDecimal("100.00"),
                        "Bonus",  "BONUS"));

        // Act — search with a non-existent user ID
        var result = transactionRepository.findByIdAndUserId(saved.getId(), 999L);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findAll_withSpecification_shouldFilterByAccountId - returns only transactions for specified account")
    void findAll_withSpecification_filtersByAccountId() {
        // Arrange — create 2 transactions on savedAccount, 1 on a different account
        transactionRepository.save(
                createTransaction(savedAccount, TransactionType.INCOME, new BigDecimal("100.00"),
                        "Income 1", "SALARY"));
        transactionRepository.save(
                createTransaction(savedAccount, TransactionType.EXPENSE, new BigDecimal("50.00"),
                        "Expense 1", "FOOD"));

        // Create a second account for a second user
        var user2 = new User();
        user2.setFirstName("Bob");
        user2.setLastName("Jones");
        user2.setEmail("bob@example.com");
        user2.setPasswordHash("hashed_password");
        var savedUser2 = userRepository.save(user2);

        AccountTypeEntity checkingType = accountTypeRepository.findByName(AccountType.CHECKING.name())
                .orElseGet(() -> accountTypeRepository.save(createAccountTypeEntity(AccountType.CHECKING.name())));

        var account2 = new Account();
        account2.setName("Bob Account");
        account2.setType(checkingType);
        account2.setBalance(new BigDecimal("2000.00"));
        account2.setCurrency("EUR");
        account2.setUser(savedUser2);
        var savedAccount2 = accountRepository.save(account2);

        transactionRepository.save(
                createTransaction(savedAccount2, TransactionType.INCOME, new BigDecimal("200.00"),
                        "Income 2",  "SALARY"));

        // Act
        var spec = TransactionSpecification.accountIdEquals(savedAccount.getId());
        var result = transactionRepository.findAll(spec, PageRequest.of(0, 10));

        // Assert
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent())
                .allMatch(t -> t.getAccount().getId().equals(savedAccount.getId()));
    }

    @Test
    @DisplayName("findAll_withSpecification_shouldFilterByType - returns only transactions matching type")
    void findAll_withSpecification_filtersByType() {
        // Arrange
        transactionRepository.save(
                createTransaction(savedAccount, TransactionType.INCOME, new BigDecimal("1000.00"),
                        "Salary",  "SALARY"));
        transactionRepository.save(
                createTransaction(savedAccount, TransactionType.INCOME, new BigDecimal("500.00"),
                        "Freelance",  "FREELANCE"));
        transactionRepository.save(
                createTransaction(savedAccount, TransactionType.EXPENSE, new BigDecimal("200.00"),
                        "Rent",  "HOUSING"));

        // Act
        var spec = TransactionSpecification.typeEquals(TransactionType.INCOME);
        var result = transactionRepository.findAll(spec, PageRequest.of(0, 10));

        // Assert
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent())
                .allMatch(t -> t.getType().getName().equals("INCOME"));
    }

    @Test
    @DisplayName("findAll_withSpecification_shouldFilterByDateBetween - returns only transactions within date range")
    void findAll_withDateBetween_filtersCorrectly() {
        // Arrange — override dates to control the range (bypass @CreationTimestamp)
        var early = transactionRepository.save(
                createTransaction(savedAccount, TransactionType.EXPENSE, new BigDecimal("10.00"),
                        "Early",  "OTHER"));
        var mid = transactionRepository.save(
                createTransaction(savedAccount, TransactionType.EXPENSE, new BigDecimal("20.00"),
                        "Mid",  "OTHER"));
        var late = transactionRepository.save(
                createTransaction(savedAccount, TransactionType.EXPENSE, new BigDecimal("30.00"),
                        "Late",  "OTHER"));
        overrideTransactionDate(early, LocalDate.of(2025, 6, 15));
        overrideTransactionDate(mid, LocalDate.of(2025, 7, 15));
        overrideTransactionDate(late, LocalDate.of(2025, 8, 1));

        // Act — filter for July 2025 only
        var from = LocalDate.of(2025, 7, 1);
        var to = LocalDate.of(2025, 7, 31);
        var spec = TransactionSpecification.dateBetween(from, to);
        var result = transactionRepository.findAll(spec, PageRequest.of(0, 10));

        // Assert
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getDescription()).isEqualTo("Mid");
    }

    @Test
    @DisplayName("findAll_withSpecification_filtersByCategory - returns only transactions matching category")
    void findAll_withSpecification_filtersByCategory() {
        // Arrange
        transactionRepository.save(
                createTransaction(savedAccount, TransactionType.INCOME, new BigDecimal("1000.00"),
                        "Salary", "SALARY"));
        transactionRepository.save(
                createTransaction(savedAccount, TransactionType.EXPENSE, new BigDecimal("20.00"),
                        "Lunch",  "FOOD"));
        transactionRepository.save(
                createTransaction(savedAccount, TransactionType.INCOME, new BigDecimal("500.00"),
                        "Freelance",  "SALARY"));

        // Act
        var spec = TransactionSpecification.categoryEquals("SALARY");
        var result = transactionRepository.findAll(spec, PageRequest.of(0, 10));

        // Assert
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent())
                .allMatch(t -> "SALARY".equals(t.getCategory().getName()));
    }

    @Test
    @DisplayName("findAll_withSpecification_filtersByDescriptionContains - returns transactions matching description via LIKE")
    void findAll_withSpecification_filtersByDescriptionContains() {
        // Arrange
        transactionRepository.save(
                createTransaction(savedAccount, TransactionType.EXPENSE, new BigDecimal("5.00"),
                        "Morning coffee",  "FOOD"));
        transactionRepository.save(
                createTransaction(savedAccount, TransactionType.EXPENSE, new BigDecimal("3.50"),
                        "Afternoon tea",  "FOOD"));
        transactionRepository.save(
                createTransaction(savedAccount, TransactionType.EXPENSE, new BigDecimal("8.00"),
                        "Coffee beans",  "FOOD"));

        // Act
        var spec = TransactionSpecification.descriptionContains("coffee");
        var result = transactionRepository.findAll(spec, PageRequest.of(0, 10));

        // Assert
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent())
                .allMatch(t -> t.getDescription().toLowerCase().contains("coffee"));
    }

    @Test
    @DisplayName("findAll_withSpecification_dateBetween_fromOnly - from is null means no lower bound")
    void findAll_withSpecification_dateBetween_fromOnly() {
        // Arrange — override dates to control the range (bypass @CreationTimestamp)
        var early = transactionRepository.save(
                createTransaction(savedAccount, TransactionType.EXPENSE, new BigDecimal("10.00"),
                        "Early",  "OTHER"));
        var mid = transactionRepository.save(
                createTransaction(savedAccount, TransactionType.EXPENSE, new BigDecimal("20.00"),
                        "Mid", "OTHER"));
        var late = transactionRepository.save(
                createTransaction(savedAccount, TransactionType.EXPENSE, new BigDecimal("30.00"),
                        "Late",  "OTHER"));
        overrideTransactionDate(early, LocalDate.of(2025, 7, 1));
        overrideTransactionDate(mid, LocalDate.of(2025, 7, 15));
        overrideTransactionDate(late, LocalDate.of(2025, 8, 1));

        // Act — only `to` bound (July 31), no `from` bound
        var spec = TransactionSpecification.dateBetween(null, LocalDate.of(2025, 7, 31));
        var result = transactionRepository.findAll(spec, PageRequest.of(0, 10));

        // Assert — returns transactions on or before July 31 (Early and Mid)
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getDescription()).isEqualTo("Early");
        assertThat(result.getContent().get(1).getDescription()).isEqualTo("Mid");
    }

    @Test
    @DisplayName("findAll_withSpecification_dateBetween_toOnly - to is null means no upper bound")
    void findAll_withSpecification_dateBetween_toOnly() {
        // Arrange — override dates to control the range (bypass @CreationTimestamp)
        var early = transactionRepository.save(
                createTransaction(savedAccount, TransactionType.EXPENSE, new BigDecimal("10.00"),
                        "Early",  "OTHER"));
        var mid = transactionRepository.save(
                createTransaction(savedAccount, TransactionType.EXPENSE, new BigDecimal("20.00"),
                        "Mid",  "OTHER"));
        var late = transactionRepository.save(
                createTransaction(savedAccount, TransactionType.EXPENSE, new BigDecimal("30.00"),
                        "Late",  "OTHER"));
        overrideTransactionDate(early, LocalDate.of(2025, 6, 15));
        overrideTransactionDate(mid, LocalDate.of(2025, 7, 15));
        overrideTransactionDate(late, LocalDate.of(2025, 8, 1));

        // Act — only `from` bound (July 1), no `to` bound
        var spec = TransactionSpecification.dateBetween(LocalDate.of(2025, 7, 1), null);
        var result = transactionRepository.findAll(spec, PageRequest.of(0, 10));

        // Assert — returns transactions on or after July 1 (Mid and Late, but not Early)
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getDescription()).isEqualTo("Mid");
        assertThat(result.getContent().get(1).getDescription()).isEqualTo("Late");
    }

    @Test
    @DisplayName("findAll_withCombinedSpecifications_filtersByTypeAndCategoryAndDate - multiple filters applied together")
    void findAll_withCombinedSpecifications_filtersByTypeAndCategoryAndDate() {
        // Arrange — override dates to control the range (bypass @CreationTimestamp)
        var salary = transactionRepository.save(
                createTransaction(savedAccount, TransactionType.INCOME, new BigDecimal("1000.00"),
                        "Salary",  "SALARY"));
        transactionRepository.save(
                createTransaction(savedAccount, TransactionType.EXPENSE, new BigDecimal("50.00"),
                        "Lunch",  "FOOD"));
        transactionRepository.save(
                createTransaction(savedAccount, TransactionType.INCOME, new BigDecimal("500.00"),
                        "Freelance",  "FREELANCE"));
        transactionRepository.save(
                createTransaction(savedAccount, TransactionType.EXPENSE, new BigDecimal("200.00"),
                        "Rent",  "HOUSING"));
        transactionRepository.save(
                createTransaction(savedAccount, TransactionType.INCOME, new BigDecimal("300.00"),
                        "Bonus",  "SALARY"));

        // Put only the Salary INCOME transaction in July 2025
        overrideTransactionDate(salary, LocalDate.of(2025, 7, 15));

        // Act — filter: INCOME type + SALARY category + July date range
        var spec = Specification.where(TransactionSpecification.typeEquals(TransactionType.INCOME))
                .and(TransactionSpecification.categoryEquals("SALARY"))
                .and(TransactionSpecification.dateBetween(LocalDate.of(2025, 7, 1), LocalDate.of(2025, 7, 31)));
        var result = transactionRepository.findAll(spec, PageRequest.of(0, 10));

        // Assert — only the July salary matches all three filters
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getDescription()).isEqualTo("Salary");
        assertThat(result.getContent().get(0).getType().getName()).isEqualTo("INCOME");
        assertThat(result.getContent().get(0).getCategory().getName()).isEqualTo("SALARY");
    }

    @Test
    @DisplayName("findAll_withCombinedSpecifications_filtersByAccountIdAndType - account + type filter")
    void findAll_withCombinedSpecifications_filtersByAccountIdAndType() {
        // Arrange
        transactionRepository.save(
                createTransaction(savedAccount, TransactionType.INCOME, new BigDecimal("100.00"),
                        "Income 1", "SALARY"));
        transactionRepository.save(
                createTransaction(savedAccount, TransactionType.EXPENSE, new BigDecimal("50.00"),
                        "Expense 1",  "FOOD"));

        var user2 = new User();
        user2.setFirstName("Bob");
        user2.setLastName("Jones");
        user2.setEmail("bob2@example.com");
        user2.setPasswordHash("hashed_password");
        var savedUser2 = userRepository.save(user2);

        AccountTypeEntity checkingType = accountTypeRepository.findByName(AccountType.CHECKING.name())
                .orElseGet(() -> accountTypeRepository.save(createAccountTypeEntity(AccountType.CHECKING.name())));

        var account2 = new Account();
        account2.setName("Bob Account");
        account2.setType(checkingType);
        account2.setBalance(new BigDecimal("2000.00"));
        account2.setCurrency("EUR");
        account2.setUser(savedUser2);
        var savedAccount2 = accountRepository.save(account2);

        transactionRepository.save(
                createTransaction(savedAccount2, TransactionType.INCOME, new BigDecimal("200.00"),
                        "Income 2",  "SALARY"));

        // Act — filter: savedAccount + INCOME
        var spec = Specification.where(TransactionSpecification.accountIdEquals(savedAccount.getId()))
                .and(TransactionSpecification.typeEquals(TransactionType.INCOME));
        var result = transactionRepository.findAll(spec, PageRequest.of(0, 10));

        // Assert — only INCOME on savedAccount
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getDescription()).isEqualTo("Income 1");
    }
}
