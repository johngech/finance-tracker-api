package com.marakicode.financetracker.reports;

import com.marakicode.financetracker.accounts.*;
import com.marakicode.financetracker.transactions.*;
import com.marakicode.financetracker.users.User;
import com.marakicode.financetracker.users.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@DisplayName("Reports Repository Tests")
class ReportsRepositoryTest {

    @Autowired
    private ReportsRepository reportsRepository;

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

    private User savedUser;
    private Account savedAccount;
    private TransactionTypeEntity incomeType;
    private TransactionTypeEntity expenseType;

    @BeforeEach
    void setUp() {
        var user = new User();
        user.setFirstName("Test");
        user.setLastName("User");
        user.setEmail("reports@example.com");
        user.setPasswordHash("hashed_password");
        savedUser = userRepository.save(user);

        AccountTypeEntity accountType = accountTypeRepository.findByName(AccountType.CHECKING.name())
                .orElseGet(() -> {
                    var t = new AccountTypeEntity();
                    t.setName(AccountType.CHECKING.name());
                    return accountTypeRepository.save(t);
                });

        var account = new Account();
        account.setName("Main Account");
        account.setType(accountType);
        account.setBalance(new BigDecimal("5000.00"));
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

    private TransactionCategoryEntity findOrCreateCategory(String name) {
        return transactionCategoryRepository.findByName(name)
                .orElseGet(() -> {
                    var entity = new TransactionCategoryEntity();
                    entity.setName(name);
                    return transactionCategoryRepository.save(entity);
                });
    }

    private Transaction createTransaction(TransactionType type, BigDecimal amount,
                                          String description, LocalDate date, String category) {
        var transaction = new Transaction();
        transaction.setAccount(savedAccount);
        transaction.setType(type == TransactionType.INCOME ? incomeType : expenseType);
        transaction.setAmount(amount);
        transaction.setDescription(description);
        transaction.setTransactionDate(date);
        if (category != null) {
            transaction.setCategory(findOrCreateCategory(category));
        }
        return transaction;
    }

    private Transaction createTransaction(Account account, TransactionType type, BigDecimal amount,
                                          String description, LocalDate date, String category) {
        var transaction = new Transaction();
        transaction.setAccount(account);
        transaction.setType(type == TransactionType.INCOME ? incomeType : expenseType);
        transaction.setAmount(amount);
        transaction.setDescription(description);
        transaction.setTransactionDate(date);
        if (category != null) {
            transaction.setCategory(findOrCreateCategory(category));
        }
        return transaction;
    }

    /**
     * H2 wraps multi-column aggregate results in a nested Object[].
     * Unwrap to a flat Object[] for consistent assertions across H2 and PostgreSQL.
     */
    private Object[] flattenResult(Object[] result) {
        if (result.length == 1 && result[0] instanceof Object[]) {
            return (Object[]) result[0];
        }
        return result;
    }

    // --- Tests for getSummaryByUserId ---

    @Test
    @DisplayName("getSummaryByUserId_withTransactions_returnsCorrectTotals - sums income and expense separately")
    void getSummaryByUserId_withTransactions_returnsCorrectTotals() {
        // Arrange
        reportsRepository.save(createTransaction(TransactionType.INCOME,
                new BigDecimal("1000.00"), "Salary", LocalDate.of(2025, 7, 1), "SALARY"));
        reportsRepository.save(createTransaction(TransactionType.INCOME,
                new BigDecimal("500.00"), "Freelance", LocalDate.of(2025, 7, 15), "FREELANCE"));
        reportsRepository.save(createTransaction(TransactionType.EXPENSE,
                new BigDecimal("200.00"), "Rent", LocalDate.of(2025, 7, 5), "HOUSING"));
        reportsRepository.save(createTransaction(TransactionType.EXPENSE,
                new BigDecimal("50.00"), "Groceries", LocalDate.of(2025, 7, 10), "FOOD"));

        // Act
        Object[] result = flattenResult(reportsRepository.getSummaryByUserId(
                savedUser.getId(), null, null));

        // Assert
        BigDecimal totalIncome = (BigDecimal) result[0];
        BigDecimal totalExpense = (BigDecimal) result[1];
        long count = ((Number) result[2]).longValue();
        assertThat(totalIncome).isEqualByComparingTo(new BigDecimal("1500.00"));
        assertThat(totalExpense).isEqualByComparingTo(new BigDecimal("250.00"));
        assertThat(count).isEqualTo(4);
    }

    @Test
    @DisplayName("getSummaryByUserId_withDateRange_filtersCorrectly - only includes transactions within range")
    void getSummaryByUserId_withDateRange_filtersCorrectly() {
        // Arrange
        reportsRepository.save(createTransaction(TransactionType.INCOME,
                new BigDecimal("1000.00"), "Salary", LocalDate.of(2025, 6, 1), "SALARY"));
        reportsRepository.save(createTransaction(TransactionType.EXPENSE,
                new BigDecimal("100.00"), "Lunch", LocalDate.of(2025, 7, 15), "FOOD"));
        reportsRepository.save(createTransaction(TransactionType.EXPENSE,
                new BigDecimal("200.00"), "Dinner", LocalDate.of(2025, 8, 1), "FOOD"));

        // Act — July only
        Object[] result = flattenResult(reportsRepository.getSummaryByUserId(
                savedUser.getId(),
                LocalDate.of(2025, 7, 1),
                LocalDate.of(2025, 7, 31)));

        // Assert
        BigDecimal totalIncome = (BigDecimal) result[0];
        BigDecimal totalExpense = (BigDecimal) result[1];
        long count = ((Number) result[2]).longValue();
        assertThat(totalIncome).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(totalExpense).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("getSummaryByUserId_noTransactions_returnsZeros - empty result set returns zeros")
    void getSummaryByUserId_noTransactions_returnsZeros() {
        // Act
        Object[] result = flattenResult(reportsRepository.getSummaryByUserId(
                savedUser.getId(), null, null));

        // Assert
        BigDecimal totalIncome = (BigDecimal) result[0];
        BigDecimal totalExpense = (BigDecimal) result[1];
        long count = ((Number) result[2]).longValue();
        assertThat(totalIncome).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(totalExpense).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(count).isEqualTo(0);
    }

    // --- Tests for getCategoryBreakdown ---

    @Test
    @DisplayName("getCategoryBreakdown_groupsByCategory - sums amounts per category")
    void getCategoryBreakdown_groupsByCategory() {
        // Arrange
        reportsRepository.save(createTransaction(TransactionType.EXPENSE,
                new BigDecimal("50.00"), "Lunch", LocalDate.of(2025, 7, 1), "FOOD"));
        reportsRepository.save(createTransaction(TransactionType.EXPENSE,
                new BigDecimal("30.00"), "Dinner", LocalDate.of(2025, 7, 2), "FOOD"));
        reportsRepository.save(createTransaction(TransactionType.EXPENSE,
                new BigDecimal("200.00"), "Rent", LocalDate.of(2025, 7, 3), "HOUSING"));

        // Act
        var results = reportsRepository.getCategoryBreakdown(
                savedUser.getId(), null, null, null);

        // Assert
        assertThat(results).hasSize(2);
        // Ordered by SUM(amount) DESC: HOUSING=200, FOOD=80
        assertThat((String) results.get(0)[0]).isEqualTo("HOUSING");
        assertThat((BigDecimal) results.get(0)[1]).isEqualByComparingTo(new BigDecimal("200.00"));
        assertThat((String) results.get(1)[0]).isEqualTo("FOOD");
        assertThat((BigDecimal) results.get(1)[1]).isEqualByComparingTo(new BigDecimal("80.00"));
    }

    @Test
    @DisplayName("getCategoryBreakdown_withTypeFilter_filtersByType - only includes transactions of specified type")
    void getCategoryBreakdown_withTypeFilter_filtersByType() {
        // Arrange
        reportsRepository.save(createTransaction(TransactionType.INCOME,
                new BigDecimal("1000.00"), "Salary", LocalDate.of(2025, 7, 1), "SALARY"));
        reportsRepository.save(createTransaction(TransactionType.EXPENSE,
                new BigDecimal("50.00"), "Food", LocalDate.of(2025, 7, 2), "FOOD"));

        // Act — INCOME only
        var results = reportsRepository.getCategoryBreakdown(
                savedUser.getId(), "INCOME", null, null);

        // Assert
        assertThat(results).hasSize(1);
        assertThat((String) results.get(0)[0]).isEqualTo("SALARY");
    }

    @Test
    @DisplayName("getCategoryBreakdown_nullCategory_returnsUncategorized - null categories grouped as Uncategorized")
    void getCategoryBreakdown_nullCategory_returnsUncategorized() {
        // Arrange
        reportsRepository.save(createTransaction(TransactionType.EXPENSE,
                new BigDecimal("25.00"), "Misc", LocalDate.of(2025, 7, 1), null));

        // Act
        var results = reportsRepository.getCategoryBreakdown(
                savedUser.getId(), null, null, null);

        // Assert
        assertThat(results).hasSize(1);
        assertThat((String) results.get(0)[0]).isEqualTo("Uncategorized");
        assertThat((BigDecimal) results.get(0)[1]).isEqualByComparingTo(new BigDecimal("25.00"));
    }

    // --- Tests for getMonthlyBreakdown ---

    @Test
    @DisplayName("getMonthlyBreakdown_groupsByMonth - returns income and expense per month")
    void getMonthlyBreakdown_groupsByMonth() {
        // Arrange
        reportsRepository.save(createTransaction(TransactionType.INCOME,
                new BigDecimal("1000.00"), "Salary", LocalDate.of(2025, 7, 1), "SALARY"));
        reportsRepository.save(createTransaction(TransactionType.EXPENSE,
                new BigDecimal("200.00"), "Rent", LocalDate.of(2025, 7, 5), "HOUSING"));
        reportsRepository.save(createTransaction(TransactionType.INCOME,
                new BigDecimal("500.00"), "Bonus", LocalDate.of(2025, 8, 1), "BONUS"));

        // Act
        var results = reportsRepository.getMonthlyBreakdown(savedUser.getId(), 2025);

        // Assert
        assertThat(results).hasSize(2);
        // Month 7 (July): income=1000, expense=200
        assertThat(((Number) results.get(0)[0]).intValue()).isEqualTo(7);
        assertThat((BigDecimal) results.get(0)[1]).isEqualByComparingTo(new BigDecimal("1000.00"));
        assertThat((BigDecimal) results.get(0)[2]).isEqualByComparingTo(new BigDecimal("200.00"));
        // Month 8 (August): income=500, expense=0
        assertThat(((Number) results.get(1)[0]).intValue()).isEqualTo(8);
        assertThat((BigDecimal) results.get(1)[1]).isEqualByComparingTo(new BigDecimal("500.00"));
        assertThat((BigDecimal) results.get(1)[2]).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("getMonthlyBreakdown_differentYear_excludesOtherYears - only includes transactions from specified year")
    void getMonthlyBreakdown_differentYear_excludesOtherYears() {
        // Arrange
        reportsRepository.save(createTransaction(TransactionType.INCOME,
                new BigDecimal("1000.00"), "Salary", LocalDate.of(2025, 7, 1), "SALARY"));
        reportsRepository.save(createTransaction(TransactionType.INCOME,
                new BigDecimal("2000.00"), "Bonus", LocalDate.of(2026, 1, 1), "BONUS"));

        // Act
        var results = reportsRepository.getMonthlyBreakdown(savedUser.getId(), 2025);

        // Assert
        assertThat(results).hasSize(1);
        assertThat(((Number) results.get(0)[0]).intValue()).isEqualTo(7);
        assertThat((BigDecimal) results.get(0)[1]).isEqualByComparingTo(new BigDecimal("1000.00"));
    }

    // --- Tests for getAccountBreakdown ---

    @Test
    @DisplayName("getAccountBreakdown_groupsByAccount - sums income and expense per account")
    void getAccountBreakdown_groupsByAccount() {
        // Arrange — create a second account
        AccountTypeEntity savingsType = accountTypeRepository.findByName(AccountType.SAVINGS.name())
                .orElseGet(() -> {
                    var t = new AccountTypeEntity();
                    t.setName(AccountType.SAVINGS.name());
                    return accountTypeRepository.save(t);
                });
        var account2 = new Account();
        account2.setName("Savings");
        account2.setType(savingsType);
        account2.setBalance(new BigDecimal("3000.00"));
        account2.setCurrency("USD");
        account2.setUser(savedUser);
        Account savedAccount2 = accountRepository.save(account2);

        reportsRepository.save(createTransaction(TransactionType.INCOME,
                new BigDecimal("1000.00"), "Salary", LocalDate.of(2025, 7, 1), "SALARY"));
        reportsRepository.save(createTransaction(TransactionType.EXPENSE,
                new BigDecimal("200.00"), "Food", LocalDate.of(2025, 7, 2), "FOOD"));
        reportsRepository.save(createTransaction(savedAccount2, TransactionType.INCOME,
                new BigDecimal("500.00"), "Interest", LocalDate.of(2025, 7, 3), "INTEREST"));

        // Act
        var results = reportsRepository.getAccountBreakdown(
                savedUser.getId(), null, null);

        // Assert
        assertThat(results).hasSize(2);
        // Ordered by account.name: "Main Account" < "Savings"
        assertThat((String) results.get(0)[1]).isEqualTo("Main Account");
        assertThat((String) results.get(1)[1]).isEqualTo("Savings");
    }

    @Test
    @DisplayName("getAccountBreakdown_withDateRange_filtersCorrectly - only includes transactions within range")
    void getAccountBreakdown_withDateRange_filtersCorrectly() {
        // Arrange
        reportsRepository.save(createTransaction(TransactionType.INCOME,
                new BigDecimal("1000.00"), "Salary", LocalDate.of(2025, 6, 1), "SALARY"));
        reportsRepository.save(createTransaction(TransactionType.EXPENSE,
                new BigDecimal("100.00"), "Food", LocalDate.of(2025, 7, 15), "FOOD"));

        // Act — July only
        var results = reportsRepository.getAccountBreakdown(
                savedUser.getId(),
                LocalDate.of(2025, 7, 1),
                LocalDate.of(2025, 7, 31));

        // Assert
        assertThat(results).hasSize(1);
        BigDecimal income = (BigDecimal) results.get(0)[2];
        BigDecimal expense = (BigDecimal) results.get(0)[3];
        assertThat(income).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(expense).isEqualByComparingTo(new BigDecimal("100.00"));
    }
}
