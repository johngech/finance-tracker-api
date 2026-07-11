package com.marakicode.financetracker.transactions;

import com.marakicode.financetracker.accounts.Account;
import com.marakicode.financetracker.common.ResourceNotFoundException;
import com.marakicode.financetracker.users.Role;
import com.marakicode.financetracker.users.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Transactions Facade Tests")
class TransactionsFacadeImplTest {

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private TransactionsFacadeImpl transactionsFacade;

    private User user;
    private Account account;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setEmail("john@example.com");
        user.setRole(Role.USER);

        account = new Account();
        account.setId(10L);
        account.setName("Main Account");
        account.setUser(user);
    }

    @Test
    @DisplayName("getTransactionById_validId_returnsTransactionSummary")
    void getTransactionById_validId_returnsTransactionSummary() {
        // Arrange
        var transaction = buildTransaction(1L, TransactionType.INCOME,
            new BigDecimal("500.00"), "Salary", LocalDate.of(2025, 6, 15), "income");

        when(transactionRepository.findById(1L))
            .thenReturn(Optional.of(transaction));

        // Act
        var result = transactionsFacade.getTransactionById(1L);

        // Assert
        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.accountId()).isEqualTo(10L);
        assertThat(result.accountName()).isEqualTo("Main Account");
        assertThat(result.userId()).isEqualTo(1L);
        assertThat(result.userName()).isEqualTo("John Doe");
        assertThat(result.type()).isEqualTo(TransactionType.INCOME);
        assertThat(result.amount()).isEqualByComparingTo(new BigDecimal("500.00"));
        assertThat(result.description()).isEqualTo("Salary");
        assertThat(result.category()).isEqualTo("income");
    }

    @Test
    @DisplayName("getTransactionById_invalidId_throwsResourceNotFoundException")
    void getTransactionById_invalidId_throwsResourceNotFoundException() {
        // Arrange
        when(transactionRepository.findById(999L))
            .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> transactionsFacade.getTransactionById(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Transaction not found");
    }

    @Test
    @DisplayName("listTransactions_noSearch_returnsPagedTransactions")
    void listTransactions_noSearch_returnsPagedTransactions() {
        // Arrange
        var t1 = buildTransaction(1L, TransactionType.INCOME,
            new BigDecimal("100.00"), "Salary", LocalDate.of(2025, 6, 1), "income");
        var t2 = buildTransaction(2L, TransactionType.EXPENSE,
            new BigDecimal("50.00"), "Groceries", LocalDate.of(2025, 6, 2), "food");
        var pageable = PageRequest.of(0, 20, Sort.by("transactionDate"));
        Page<Transaction> page = new PageImpl<>(List.of(t1, t2), pageable, 2);

        when(transactionRepository.findAll(
            any(Specification.class), any(PageRequest.class)))
            .thenReturn(page);

        // Act
        var result = transactionsFacade.listTransactions(null, pageable);

        // Assert
        assertThat(result.content()).hasSize(2);
        assertThat(result.page()).isEqualTo(0);
        assertThat(result.size()).isEqualTo(20);
        assertThat(result.totalPages()).isEqualTo(1);
        assertThat(result.content().get(0).type()).isEqualTo(TransactionType.INCOME);
        assertThat(result.content().get(1).type()).isEqualTo(TransactionType.EXPENSE);
    }

    @Test
    @DisplayName("listTransactions_withSearch_returnsFilteredTransactions")
    void listTransactions_withSearch_returnsFilteredTransactions() {
        // Arrange
        var t1 = buildTransaction(1L, TransactionType.EXPENSE,
            new BigDecimal("30.00"), "Coffee shop", LocalDate.of(2025, 6, 10), "food");
        var pageable = PageRequest.of(0, 20);
        Page<Transaction> page = new PageImpl<>(List.of(t1), pageable, 1);

        when(transactionRepository.findAll(
            any(Specification.class), any(PageRequest.class)))
            .thenReturn(page);

        // Act
        var result = transactionsFacade.listTransactions("Coffee", pageable);

        // Assert
        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).description()).isEqualTo("Coffee shop");
    }

    @Test
    @DisplayName("listUserTransactions_validUserId_returnsUserTransactions")
    void listUserTransactions_validUserId_returnsUserTransactions() {
        // Arrange
        var t1 = buildTransaction(1L, TransactionType.INCOME,
            new BigDecimal("1000.00"), "Bonus", LocalDate.of(2025, 7, 1), "income");
        var pageable = PageRequest.of(0, 10);
        Page<Transaction> page = new PageImpl<>(List.of(t1), pageable, 1);

        when(transactionRepository.findAll(
            any(Specification.class), any(PageRequest.class)))
            .thenReturn(page);

        // Act
        var result = transactionsFacade.listUserTransactions(1L, pageable);

        // Assert
        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).userId()).isEqualTo(1L);
        assertThat(result.content().get(0).userName()).isEqualTo("John Doe");
    }

    @Test
    @DisplayName("getStatistics_returnsCorrectCounts")
    void getStatistics_returnsCorrectCounts() {
        // Arrange
        when(transactionRepository.countByTypeName())
            .thenReturn(List.of(new Object[]{"INCOME", 5L}, new Object[]{"EXPENSE", 3L}));
        when(transactionRepository.sumAmountByTypeName())
            .thenReturn(List.of(
                new Object[]{"INCOME", new BigDecimal("5000.00")},
                new Object[]{"EXPENSE", new BigDecimal("2000.00")}));

        // Act
        var result = transactionsFacade.getStatistics();

        // Assert
        assertThat(result.totalTransactions()).isEqualTo(8);
        assertThat(result.incomeCount()).isEqualTo(5);
        assertThat(result.expenseCount()).isEqualTo(3);
        assertThat(result.totalIncome()).isEqualByComparingTo(new BigDecimal("5000.00"));
        assertThat(result.totalExpense()).isEqualByComparingTo(new BigDecimal("2000.00"));
        assertThat(result.netAmount()).isEqualByComparingTo(new BigDecimal("3000.00"));
    }

    @Test
    @DisplayName("getStatistics_emptyRepository_returnsZeros")
    void getStatistics_emptyRepository_returnsZeros() {
        // Arrange
        when(transactionRepository.countByTypeName()).thenReturn(List.of());
        when(transactionRepository.sumAmountByTypeName()).thenReturn(List.of());

        // Act
        var result = transactionsFacade.getStatistics();

        // Assert
        assertThat(result.totalTransactions()).isZero();
        assertThat(result.incomeCount()).isZero();
        assertThat(result.expenseCount()).isZero();
        assertThat(result.totalIncome()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.totalExpense()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.netAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    private Transaction buildTransaction(Long id, TransactionType type,
                                         BigDecimal amount, String description,
                                         LocalDate date, String categoryName) {
        var t = new Transaction();
        t.setId(id);
        t.setAccount(account);
        t.setType(buildTypeEntity(type.name()));
        t.setAmount(amount);
        t.setDescription(description);
        t.setTransactionDate(date);
        t.setCategory(categoryName != null ? buildCategoryEntity(categoryName) : null);
        return t;
    }

    private TransactionTypeEntity buildTypeEntity(String name) {
        var entity = new TransactionTypeEntity();
        entity.setName(name);
        return entity;
    }

    private TransactionCategoryEntity buildCategoryEntity(String name) {
        var entity = new TransactionCategoryEntity();
        entity.setName(name);
        return entity;
    }

    @Test
    @DisplayName("listUserTransactions_emptyResult_returnsEmptyPage")
    void listUserTransactions_emptyResult_returnsEmptyPage() {
        var pageable = PageRequest.of(0, 10);
        Page<Transaction> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);
        when(transactionRepository.findAll(
            any(Specification.class), any(PageRequest.class)))
            .thenReturn(emptyPage);

        var result = transactionsFacade.listUserTransactions(1L, pageable);

        assertThat(result.content()).isEmpty();
    }
}
