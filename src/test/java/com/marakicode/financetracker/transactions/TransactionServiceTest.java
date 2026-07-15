package com.marakicode.financetracker.transactions;

import com.marakicode.financetracker.accounts.Account;
import com.marakicode.financetracker.accounts.AccountRepository;
import com.marakicode.financetracker.common.CurrentUserProvider;
import com.marakicode.financetracker.common.PagedResponse;
import com.marakicode.financetracker.common.ResourceNotFoundException;
import com.marakicode.financetracker.transactions.dto.TransactionCreateRequest;
import com.marakicode.financetracker.transactions.dto.TransactionResponse;
import com.marakicode.financetracker.transactions.dto.TransactionUpdateRequest;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Transaction Service Tests")
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private TransactionTypeRepository transactionTypeRepository;

    @Mock
    private TransactionCategoryRepository transactionCategoryRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @Mock
    private TransactionMapper transactionMapper;

    @InjectMocks
    private TransactionService transactionService;

    private User user;
    private Account account;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");
        user.setRole(Role.USER);

        account = new Account();
        account.setId(1L);
        account.setName("Test Account");
        account.setBalance(new BigDecimal("1000.00"));
        account.setUser(user);

        lenient().when(currentUserProvider.getCurrentUserId()).thenReturn(1L);

        lenient().when(transactionTypeRepository.findByName("INCOME"))
                .thenReturn(Optional.of(createTypeEntity("INCOME")));
        lenient().when(transactionTypeRepository.findByName("EXPENSE"))
                .thenReturn(Optional.of(createTypeEntity("EXPENSE")));
        lenient().when(transactionCategoryRepository.findByName(any()))
                .thenAnswer(invocation -> {
                    String name = invocation.getArgument(0);
                    return Optional.of(createCategoryEntity(name));
                });
    }

    private TransactionTypeEntity createTypeEntity(String name) {
        var entity = new TransactionTypeEntity();
        entity.setName(name);
        return entity;
    }

    private TransactionCategoryEntity createCategoryEntity(String name) {
        var entity = new TransactionCategoryEntity();
        entity.setName(name);
        return entity;
    }

    private Transaction sampleTransaction(TransactionType type, BigDecimal amount) {
        Transaction t = new Transaction();
        t.setId(1L);
        t.setAccount(account);
        t.setType(createTypeEntity(type.name()));
        t.setAmount(amount);
        t.setDescription("test");
        t.setTransactionDate(LocalDate.of(2025, 6, 15));
        return t;
    }

    private TransactionResponse sampleResponse(TransactionType type, BigDecimal amount) {
        return new TransactionResponse(1L, 1L, "Test Account", type, amount, "test",
                LocalDate.of(2025, 6, 15), "food", null);
    }

    @Test
    @DisplayName("createTransaction_income_updatesBalanceCorrectly - adds INCOME amount to account balance")
    void createTransaction_income_updatesBalanceCorrectly() {
        // Arrange
        var request = new TransactionCreateRequest(1L, TransactionType.INCOME, new BigDecimal("200.00"), "salary", "income");
        var transaction = sampleTransaction(TransactionType.INCOME, new BigDecimal("200.00"));
        var response = sampleResponse(TransactionType.INCOME, new BigDecimal("200.00"));

        when(accountRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(account));
        when(transactionMapper.toEntity(request)).thenReturn(transaction);
        when(transactionRepository.save(transaction)).thenReturn(transaction);
        when(transactionMapper.toResponse(transaction)).thenReturn(response);

        // Act
        var result = transactionService.createTransaction(request);

        // Assert
        assertThat(result).isEqualTo(response);
        assertThat(account.getBalance()).isEqualByComparingTo(new BigDecimal("1200.00"));
        verify(transactionRepository).save(transaction);
        verify(accountRepository).save(account);
    }

    @Test
    @DisplayName("createTransaction_expense_updatesBalanceCorrectly - subtracts EXPENSE amount from account balance")
    void createTransaction_expense_updatesBalanceCorrectly() {
        // Arrange
        var request = new TransactionCreateRequest(1L, TransactionType.EXPENSE, new BigDecimal("200.00"), "groceries", "food");
        var transaction = sampleTransaction(TransactionType.EXPENSE, new BigDecimal("200.00"));
        var response = sampleResponse(TransactionType.EXPENSE, new BigDecimal("200.00"));

        when(accountRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(account));
        when(transactionMapper.toEntity(request)).thenReturn(transaction);
        when(transactionRepository.save(transaction)).thenReturn(transaction);
        when(transactionMapper.toResponse(transaction)).thenReturn(response);

        // Act
        var result = transactionService.createTransaction(request);

        // Assert
        assertThat(result).isEqualTo(response);
        assertThat(account.getBalance()).isEqualByComparingTo(new BigDecimal("800.00"));
        verify(transactionRepository).save(transaction);
        verify(accountRepository).save(account);
    }

    @Test
    @DisplayName("createTransaction_expense_insufficientFunds_throwsException - EXPENSE exceeds account balance")
    void createTransaction_expense_insufficientFunds_throwsException() {
        // Arrange
        account.setBalance(new BigDecimal("100.00"));
        var request = new TransactionCreateRequest(1L, TransactionType.EXPENSE, new BigDecimal("200.00"), "car", "transport");
        var transaction = sampleTransaction(TransactionType.EXPENSE, new BigDecimal("200.00"));

        when(accountRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(account));
        when(transactionMapper.toEntity(request)).thenReturn(transaction);

        // Act & Assert
        assertThatThrownBy(() -> transactionService.createTransaction(request))
                .isInstanceOf(InsufficientFundsException.class)
                .hasMessageContaining("Insufficient funds");
    }

    @Test
    @DisplayName("createTransaction_accountNotFound_throwsResourceNotFoundException - account does not exist for user")
    void createTransaction_accountNotFound_throwsResourceNotFoundException() {
        // Arrange
        var request = new TransactionCreateRequest(999L, TransactionType.EXPENSE, new BigDecimal("200.00"), "car", "transport");

        when(accountRepository.findByIdAndUserId(999L, 1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> transactionService.createTransaction(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Account not found");
    }

    @Test
    @DisplayName("createTransaction_nullCategory_succeeds - transaction with null category persists without category entity")
    void createTransaction_nullCategory_succeeds() {
        // Arrange
        var request = new TransactionCreateRequest(1L, TransactionType.INCOME,
                new BigDecimal("200.00"), "salary",  null);
        var transaction = sampleTransaction(TransactionType.INCOME, new BigDecimal("200.00"));
        var response = sampleResponse(TransactionType.INCOME, new BigDecimal("200.00"));

        when(accountRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(account));
        when(transactionMapper.toEntity(request)).thenReturn(transaction);
        when(transactionRepository.save(transaction)).thenReturn(transaction);
        when(transactionMapper.toResponse(transaction)).thenReturn(response);

        // Act
        var result = transactionService.createTransaction(request);

        // Assert
        assertThat(result).isEqualTo(response);
        verify(transactionCategoryRepository, never()).findByName(any());
        verify(transactionCategoryRepository, never()).insertIfAbsent(any());
        verify(transactionRepository).save(transaction);
    }

    @Test
    @DisplayName("createTransaction_invalidType_throwsIllegalArgumentException - unknown type entity throws")
    void createTransaction_invalidType_throwsIllegalArgumentException() {
        // Arrange
        when(transactionTypeRepository.findByName("INCOME")).thenReturn(Optional.empty());
        var request = new TransactionCreateRequest(1L, TransactionType.INCOME,
                new BigDecimal("200.00"), "salary",  null);

        when(accountRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(account));
        when(transactionMapper.toEntity(request)).thenReturn(sampleTransaction(TransactionType.INCOME, new BigDecimal("200.00")));

        // Act & Assert
        assertThatThrownBy(() -> transactionService.createTransaction(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown transaction type");
    }

    @Test
    @DisplayName("getTransactionById_whenOwned_returnsResponse - returns transaction owned by the user")
    void getTransactionById_whenOwned_returnsResponse() {
        // Arrange
        var transaction = sampleTransaction(TransactionType.INCOME, new BigDecimal("500.00"));
        var response = sampleResponse(TransactionType.INCOME, new BigDecimal("500.00"));

        when(transactionRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(transaction));
        when(transactionMapper.toResponse(transaction)).thenReturn(response);

        // Act
        var result = transactionService.getTransactionById(1L);

        // Assert
        assertThat(result).isEqualTo(response);
        verify(transactionMapper).toResponse(transaction);
    }

    @Test
    @DisplayName("getTransactionById_whenNotOwned_throwsResourceNotFoundException - transaction not found for user")
    void getTransactionById_whenNotOwned_throwsResourceNotFoundException() {
        // Arrange
        when(transactionRepository.findByIdAndUserId(999L, 1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> transactionService.getTransactionById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Transaction not found");
    }

    @Test
    @DisplayName("getTransactions_returnsPagedResponse - returns paginated transactions for the user")
    void getTransactions_returnsPagedResponse() {
        // Arrange
        var transaction = sampleTransaction(TransactionType.EXPENSE, new BigDecimal("50.00"));
        var response = sampleResponse(TransactionType.EXPENSE, new BigDecimal("50.00"));
        var pageable = PageRequest.of(0, 10, Sort.by("transactionDate"));
        Page<Transaction> page = new PageImpl<>(List.of(transaction), pageable, 1);

        when(transactionRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

        // Act
        var result = transactionService.getTransactions(null, null, null, null, null, null, pageable);

        // Assert
        assertThat(result.content()).hasSize(1);
        assertThat(result.page()).isEqualTo(0);
        assertThat(result.size()).isEqualTo(10);
        assertThat(result.totalPages()).isEqualTo(1);
    }

    @Test
    @DisplayName("updateTransaction_updatesFieldsCorrectly - changes type from EXPENSE to INCOME and adjusts balance")
    void updateTransaction_updatesFieldsCorrectly() {
        // Arrange: existing is EXPENSE 100, account balance 1000
        // Reverse EXPENSE: balance = 1000 + 100 = 1100
        // Apply INCOME: balance = 1100 + 100 = 1200
        var existing = sampleTransaction(TransactionType.EXPENSE, new BigDecimal("100.00"));
        var request = new TransactionUpdateRequest(TransactionType.INCOME, null, null, null);
        var response = sampleResponse(TransactionType.INCOME, new BigDecimal("100.00"));

        when(transactionRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(existing));
        when(transactionRepository.save(existing)).thenReturn(existing);
        when(accountRepository.save(account)).thenReturn(account);
        when(transactionMapper.toResponse(existing)).thenReturn(response);

        // Act
        var result = transactionService.updateTransaction(1L, request);

        // Assert
        assertThat(result).isEqualTo(response);
        assertThat(existing.getType().getName()).isEqualTo("INCOME");
        assertThat(account.getBalance()).isEqualByComparingTo(new BigDecimal("1200.00"));
        verify(transactionRepository).save(existing);
        verify(accountRepository).save(account);
    }

    @Test
    @DisplayName("updateTransaction_allNullFields_returnsUnchanged - no persistence when all update fields null")
    void updateTransaction_allNullFields_returnsUnchanged() {
        // Arrange
        var existing = sampleTransaction(TransactionType.EXPENSE, new BigDecimal("100.00"));
        var request = new TransactionUpdateRequest(null, null, null, null);
        var response = sampleResponse(TransactionType.EXPENSE, new BigDecimal("100.00"));

        when(transactionRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(existing));
        when(transactionMapper.toResponse(existing)).thenReturn(response);

        // Act
        var result = transactionService.updateTransaction(1L, request);

        // Assert
        assertThat(result).isEqualTo(response);
        assertThat(account.getBalance()).isEqualByComparingTo(new BigDecimal("1000.00"));
        verify(transactionRepository, never()).save(any());
        verify(accountRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateTransaction_expense_insufficientFunds_throwsException - updated EXPENSE exceeds reversed balance")
    void updateTransaction_expense_insufficientFunds_throwsException() {
        // Arrange: existing is INCOME 500, account balance 1000
        // Reverse INCOME: balance = 1000 - 500 = 500
        // Update to EXPENSE 2000: 2000 > 500 → insufficient
        var existing = sampleTransaction(TransactionType.INCOME, new BigDecimal("500.00"));
        var request = new TransactionUpdateRequest(TransactionType.EXPENSE, new BigDecimal("2000.00"),
                null, null);

        when(transactionRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(existing));

        // Act & Assert
        assertThatThrownBy(() -> transactionService.updateTransaction(1L, request))
                .isInstanceOf(InsufficientFundsException.class)
                .hasMessageContaining("Insufficient funds");
    }

    @Test
    @DisplayName("updateTransaction_accountNotFound_throwsResourceNotFoundException - transaction not found for user")
    void updateTransaction_accountNotFound_throwsResourceNotFoundException() {
        // Arrange
        var request = new TransactionUpdateRequest(TransactionType.INCOME, null, null, null);

        when(transactionRepository.findByIdAndUserId(999L, 1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> transactionService.updateTransaction(999L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Transaction not found");
    }

    @Test
    @DisplayName("deleteTransaction_reversesBalanceAndDeletes - restores account balance and removes transaction")
    void deleteTransaction_reversesBalanceAndDeletes() {
        // Arrange: deleting EXPENSE 200 → balance 1000 + 200 = 1200
        var transaction = sampleTransaction(TransactionType.EXPENSE, new BigDecimal("200.00"));

        when(transactionRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(transaction));

        // Act
        transactionService.deleteTransaction(1L);

        // Assert
        assertThat(account.getBalance()).isEqualByComparingTo(new BigDecimal("1200.00"));
        verify(accountRepository).save(account);
        verify(transactionRepository).delete(transaction);
    }

    @Test
    @DisplayName("deleteTransaction_notFound_throwsResourceNotFoundException - transaction does not exist for user")
    void deleteTransaction_notFound_throwsResourceNotFoundException() {
        // Arrange
        when(transactionRepository.findByIdAndUserId(999L, 1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> transactionService.deleteTransaction(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Transaction not found");
    }

    @Test
    @DisplayName("createTransaction_savesTransactionBeforeBalanceUpdate - verifies transaction save precedes account save")
    void createTransaction_savesTransactionBeforeBalanceUpdate() {
        // Arrange
        var request = new TransactionCreateRequest(1L, TransactionType.INCOME, new BigDecimal("200.00"), "salary", "income");
        var transaction = sampleTransaction(TransactionType.INCOME, new BigDecimal("200.00"));
        var response = sampleResponse(TransactionType.INCOME, new BigDecimal("200.00"));

        when(accountRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(account));
        when(transactionMapper.toEntity(request)).thenReturn(transaction);
        when(transactionRepository.save(transaction)).thenReturn(transaction);
        when(transactionMapper.toResponse(transaction)).thenReturn(response);

        // Act
        transactionService.createTransaction(request);

        // Assert
        var inOrder = inOrder(transactionRepository, accountRepository);
        inOrder.verify(transactionRepository).save(transaction);
        inOrder.verify(accountRepository).save(account);
    }

    @Test
    @DisplayName("createTransaction_expense_exactBalance_succeeds - EXPENSE equal to balance is allowed (compareTo < 0 boundary)")
    void createTransaction_expense_exactBalance_succeeds() {
        // Arrange
        account.setBalance(new BigDecimal("200.00"));
        var request = new TransactionCreateRequest(1L, TransactionType.EXPENSE, new BigDecimal("200.00"), "rent", "housing");
        var transaction = sampleTransaction(TransactionType.EXPENSE, new BigDecimal("200.00"));
        var response = sampleResponse(TransactionType.EXPENSE, new BigDecimal("200.00"));

        when(accountRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(account));
        when(transactionMapper.toEntity(request)).thenReturn(transaction);
        when(transactionRepository.save(transaction)).thenReturn(transaction);
        when(transactionMapper.toResponse(transaction)).thenReturn(response);

        // Act
        var result = transactionService.createTransaction(request);

        // Assert
        assertThat(result).isEqualTo(response);
        assertThat(account.getBalance()).isEqualByComparingTo(new BigDecimal("0.00"));
        verify(transactionRepository).save(transaction);
        verify(accountRepository).save(account);
    }

    @Test
    @DisplayName("deleteTransaction_income_reversesBalanceCorrectly - deleting INCOME subtracts from balance")
    void deleteTransaction_income_reversesBalanceCorrectly() {
        // Arrange: deleting INCOME 300 → balance 1000 - 300 = 700
        var transaction = sampleTransaction(TransactionType.INCOME, new BigDecimal("300.00"));

        when(transactionRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(transaction));

        // Act
        transactionService.deleteTransaction(1L);

        // Assert
        assertThat(account.getBalance()).isEqualByComparingTo(new BigDecimal("700.00"));
        verify(accountRepository).save(account);
        verify(transactionRepository).delete(transaction);
    }

    @Test
    @DisplayName("updateTransaction_sameType_updatesAmount - changes amount within same type, balance adjusts correctly")
    void updateTransaction_sameType_updatesAmount() {
        // Arrange: existing is EXPENSE 100, balance 1000
        // Reverse EXPENSE: balance = 1000 + 100 = 1100
        // Apply EXPENSE 50: balance = 1100 - 50 = 1050
        var existing = sampleTransaction(TransactionType.EXPENSE, new BigDecimal("100.00"));
        var request = new TransactionUpdateRequest(TransactionType.EXPENSE, new BigDecimal("50.00"),
                null, null);
        var response = sampleResponse(TransactionType.EXPENSE, new BigDecimal("50.00"));

        when(transactionRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(existing));
        when(transactionRepository.save(existing)).thenReturn(existing);
        when(accountRepository.save(account)).thenReturn(account);
        when(transactionMapper.toResponse(existing)).thenReturn(response);

        // Act
        var result = transactionService.updateTransaction(1L, request);

        // Assert
        assertThat(result).isEqualTo(response);
        assertThat(existing.getType().getName()).isEqualTo("EXPENSE");
        assertThat(existing.getAmount()).isEqualByComparingTo(new BigDecimal("50.00"));
        assertThat(account.getBalance()).isEqualByComparingTo(new BigDecimal("1050.00"));
        verify(transactionRepository).save(existing);
        verify(accountRepository).save(account);
    }

    @Test
    @DisplayName("updateTransaction_nonFinancialFields_onlyUpdatesMetadata - description/category update without balance change")
    void updateTransaction_nonFinancialFields_onlyUpdatesMetadata() {
        // Arrange: existing is EXPENSE 100, balance 1000
        // Only description and category change — type and amount remain null in request
        // Balance should NOT be touched (no reverse/re-apply)
        var existing = sampleTransaction(TransactionType.EXPENSE, new BigDecimal("100.00"));
        existing.setCategory(createCategoryEntity("food"));
        var request = new TransactionUpdateRequest(null, null, "updated description", "Transport");
        var response = sampleResponse(TransactionType.EXPENSE, new BigDecimal("100.00"));

        when(transactionRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(existing));
        when(transactionRepository.save(existing)).thenReturn(existing);
        when(transactionMapper.toResponse(existing)).thenReturn(response);

        // Act
        var result = transactionService.updateTransaction(1L, request);

        // Assert
        assertThat(result).isEqualTo(response);
        assertThat(existing.getDescription()).isEqualTo("updated description");
        assertThat(existing.getCategory().getName()).isEqualTo("Transport");
        assertThat(existing.getType().getName()).isEqualTo("EXPENSE");
        assertThat(existing.getAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(account.getBalance()).isEqualByComparingTo(new BigDecimal("1000.00"));
        verify(transactionRepository).save(existing);
        verify(accountRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateTransaction_onlyDescriptionUpdate_skipsBalanceMutation - no balance operation for metadata-only change")
    void updateTransaction_onlyDescriptionUpdate_skipsBalanceMutation() {
        // Arrange: only description changes, balance must not be touched
        var existing = sampleTransaction(TransactionType.EXPENSE, new BigDecimal("100.00"));
        var request = new TransactionUpdateRequest(null, null, "new desc", null);
        var response = sampleResponse(TransactionType.EXPENSE, new BigDecimal("100.00"));

        when(transactionRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(existing));
        when(transactionRepository.save(existing)).thenReturn(existing);
        when(transactionMapper.toResponse(existing)).thenReturn(response);

        // Act
        transactionService.updateTransaction(1L, request);

        // Assert — no balance operations
        verify(accountRepository, never()).save(any());
        assertThat(account.getBalance()).isEqualByComparingTo(new BigDecimal("1000.00"));
    }

    @Test
    @DisplayName("updateTransaction_bothTypeAndAmountChange_adjustsBalance - changes EXPENSE 100 to INCOME 200")
    void updateTransaction_bothTypeAndAmountChange_adjustsBalance() {
        // Arrange: existing is EXPENSE 100, account balance 1000
        // Reverse EXPENSE: balance = 1000 + 100 = 1100
        // Apply INCOME 200: balance = 1100 + 200 = 1300
        var existing = sampleTransaction(TransactionType.EXPENSE, new BigDecimal("100.00"));
        var request = new TransactionUpdateRequest(TransactionType.INCOME, new BigDecimal("200.00"),
                null, null);
        var response = sampleResponse(TransactionType.INCOME, new BigDecimal("200.00"));

        when(transactionRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(existing));
        when(transactionRepository.save(existing)).thenReturn(existing);
        when(accountRepository.save(account)).thenReturn(account);
        when(transactionMapper.toResponse(existing)).thenReturn(response);

        // Act
        var result = transactionService.updateTransaction(1L, request);

        // Assert
        assertThat(result).isEqualTo(response);
        assertThat(existing.getType().getName()).isEqualTo("INCOME");
        assertThat(existing.getAmount()).isEqualByComparingTo(new BigDecimal("200.00"));
        assertThat(account.getBalance()).isEqualByComparingTo(new BigDecimal("1300.00"));
        verify(transactionRepository).save(existing);
        verify(accountRepository).save(account);
    }

    @Test
    @DisplayName("createTransaction_newCategory_persistsCategory - resolves category via find-or-create flow")
    void createTransaction_newCategory_persistsCategory() {
        // Arrange: findByName returns empty first, then present after insertIfAbsent
        var newCategoryName = "NEW_CAT";
        when(transactionCategoryRepository.findByName(newCategoryName))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(createCategoryEntity(newCategoryName)));

        var request = new TransactionCreateRequest(1L, TransactionType.INCOME,
                new BigDecimal("200.00"), "salary", newCategoryName);
        var transaction = sampleTransaction(TransactionType.INCOME, new BigDecimal("200.00"));
        var response = sampleResponse(TransactionType.INCOME, new BigDecimal("200.00"));

        when(accountRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(account));
        when(transactionMapper.toEntity(request)).thenReturn(transaction);
        when(transactionRepository.save(transaction)).thenReturn(transaction);
        when(transactionMapper.toResponse(transaction)).thenReturn(response);

        // Act
        var result = transactionService.createTransaction(request);

        // Assert
        assertThat(result).isEqualTo(response);
        verify(transactionCategoryRepository).insertIfAbsent(newCategoryName);
        verify(transactionRepository).save(transaction);
    }

    @Test
    @DisplayName("createTransaction_categoryCreationFails_throwsIllegalStateException - second findByName returns empty")
    void createTransaction_categoryCreationFails_throwsIllegalStateException() {
        // Arrange: findByName always returns empty, even after insertIfAbsent
        var failCategoryName = "BROKEN_CAT";
        when(transactionCategoryRepository.findByName(failCategoryName))
                .thenReturn(Optional.empty());

        var request = new TransactionCreateRequest(1L, TransactionType.INCOME,
                new BigDecimal("200.00"), "salary", failCategoryName);
        var transaction = sampleTransaction(TransactionType.INCOME, new BigDecimal("200.00"));

        when(accountRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(account));
        when(transactionMapper.toEntity(request)).thenReturn(transaction);

        // Act & Assert
        assertThatThrownBy(() -> transactionService.createTransaction(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Failed to create category");
    }

    @Test
    @DisplayName("deleteTransaction_nullCategory_succeeds - transaction without category deletes cleanly")
    void deleteTransaction_nullCategory_succeeds() {
        // Arrange: deleting INCOME 300 → balance 1000 - 300 = 700
        var transaction = sampleTransaction(TransactionType.INCOME, new BigDecimal("300.00"));
        transaction.setCategory(null);

        when(transactionRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(transaction));

        // Act
        transactionService.deleteTransaction(1L);

        // Assert
        assertThat(account.getBalance()).isEqualByComparingTo(new BigDecimal("700.00"));
        verify(accountRepository).save(account);
        verify(transactionRepository).delete(transaction);
    }

    @Test
    @DisplayName("createTransaction_frozenAccount_throwsAccountFrozenException - rejects transactions on frozen accounts")
    void createTransaction_frozenAccount_throwsAccountFrozenException() {
        // Arrange
        account.setFrozen(true);
        var request = new TransactionCreateRequest(1L, TransactionType.INCOME, new BigDecimal("200.00"), "salary", "income");

        when(accountRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(account));

        // Act & Assert
        assertThatThrownBy(() -> transactionService.createTransaction(request))
                .isInstanceOf(AccountFrozenException.class)
                .hasMessageContaining("account is frozen");
    }
}
