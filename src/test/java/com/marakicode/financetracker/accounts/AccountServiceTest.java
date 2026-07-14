package com.marakicode.financetracker.accounts;

import com.marakicode.financetracker.accounts.dto.AccountCreateRequest;
import com.marakicode.financetracker.accounts.dto.AccountResponse;
import com.marakicode.financetracker.accounts.dto.CurrencyUpdateRequest;
import com.marakicode.financetracker.accounts.dto.UpdateAccountTypeRequest;
import com.marakicode.financetracker.common.CurrentUserProvider;
import com.marakicode.financetracker.common.DuplicateResourceException;
import com.marakicode.financetracker.common.PagedResponse;
import com.marakicode.financetracker.common.ResourceNotFoundException;
import com.marakicode.financetracker.users.User;
import com.marakicode.financetracker.users.UserRepository;
import com.marakicode.financetracker.users.UserService;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountTypeRepository accountTypeRepository;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AccountMapper accountMapper;

    @InjectMocks
    private AccountService accountService;

    @Mock
    private UserService userService;

    private AccountTypeEntity checkingEntity;
    private User sampleUser;

    @BeforeEach
    void setUp() {
        checkingEntity = new AccountTypeEntity();
        checkingEntity.setId(1L);
        checkingEntity.setName("CHECKING");

        sampleUser = new User();
        sampleUser.setId(1L);
        sampleUser.setFirstName("Alice");
        sampleUser.setLastName("Smith");
        sampleUser.setEmail("alice@example.com");

        lenient().when(currentUserProvider.getCurrentUserId()).thenReturn(1L);
        lenient().when(userService.getReferenceById(1L)).thenReturn(sampleUser);
    }

    private Account sampleAccount() {
        Account account = new Account();
        account.setId(1L);
        account.setName("Checking123");
        account.setType(checkingEntity);
        account.setBalance(new BigDecimal("1000.00"));
        account.setCurrency("USD");
        return account;
    }

    private static AccountResponse sampleAccountResponse() {
        return new AccountResponse(1L, "Checking123", AccountType.CHECKING,
                new BigDecimal("1000.00"), "USD", LocalDateTime.of(2025, 1, 15, 10, 30));
    }

    @Test
    @DisplayName("createAccount_shouldReturnResponse_whenValidRequest - creates account with user from current user provider")
    void createAccount_shouldReturnResponse_whenValidRequest() {
        // Arrange
        var request = new AccountCreateRequest("Checking123", AccountType.CHECKING, "USD", new BigDecimal("1000.00"));
        var account = sampleAccount();
        var response = sampleAccountResponse();

        lenient().when(accountRepository.existsByUserIdAndName(1L, "Checking123")).thenReturn(false);
        lenient().when(accountMapper.toEntity(request)).thenReturn(account);
        lenient().when(accountTypeRepository.findByName("CHECKING")).thenReturn(Optional.of(checkingEntity));
        lenient().when(accountRepository.save(account)).thenReturn(account);
        lenient().when(accountMapper.toResponse(account)).thenReturn(response);

        // Act
        var result = accountService.createAccount(request);

        // Assert
        assertThat(result).isEqualTo(response);
        assertThat(account.getUser()).isEqualTo(sampleUser);
        assertThat(account.getBalance()).isEqualByComparingTo(new BigDecimal("1000.00"));
        verify(accountRepository).save(account);
    }

    @Test
    @DisplayName("getAccountById_shouldReturnResponse_whenFoundAndOwned - returns account owned by the user")
    void getAccountById_shouldReturnResponse_whenFoundAndOwned() {
        // Arrange
        var account = sampleAccount();
        var response = sampleAccountResponse();

        when(accountRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(account));
        when(accountMapper.toResponse(account)).thenReturn(response);

        // Act
        var result = accountService.getAccountById(1L);

        // Assert
        assertThat(result).isEqualTo(response);
    }

    @Test
    @DisplayName("getAccountById_shouldThrow_whenNotFound - throws for nonexistent id or not owned")
    void getAccountById_shouldThrow_whenNotFound() {
        // Arrange
        when(accountRepository.findByIdAndUserId(999L, 1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> accountService.getAccountById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Account not found");
    }

    @Test
    @DisplayName("getAccounts_shouldReturnPagedResponse - returns paginated accounts for the user with default sort")
    void getAccounts_shouldReturnPagedResponse() {
        // Arrange
        var account = sampleAccount();
        var response = sampleAccountResponse();
        var pageable = PageRequest.of(0, 10, Sort.by("name"));
        Page<Account> page = new PageImpl<>(List.of(account), pageable, 1);

        when(accountRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);
        when(accountMapper.toResponse(account)).thenReturn(response);

        // Act
        var result = accountService.getAccounts(null, null, null, pageable);

        // Assert
        assertThat(result.content()).hasSize(1);
        assertThat(result.page()).isEqualTo(0);
        assertThat(result.size()).isEqualTo(10);
        assertThat(result.totalPages()).isEqualTo(1);
    }

    @Test
    @DisplayName("getAccounts_shouldFilterBySearch - filters accounts by name search term")
    void getAccounts_shouldFilterBySearch() {
        // Arrange
        var account = sampleAccount();
        var response = sampleAccountResponse();
        var pageable = PageRequest.of(0, 10, Sort.by("name"));
        Page<Account> page = new PageImpl<>(List.of(account), pageable, 1);

        when(accountRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);
        when(accountMapper.toResponse(account)).thenReturn(response);

        // Act
        var result = accountService.getAccounts("Checking123", null, null, pageable);

        // Assert
        assertThat(result.content()).hasSize(1);
    }

    @Test
    @DisplayName("getAccounts_shouldFilterByType - filters accounts by account type")
    void getAccounts_shouldFilterByType() {
        // Arrange
        var account = sampleAccount();
        var response = sampleAccountResponse();
        var pageable = PageRequest.of(0, 10, Sort.by("name"));
        Page<Account> page = new PageImpl<>(List.of(account), pageable, 1);

        when(accountRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);
        when(accountMapper.toResponse(account)).thenReturn(response);

        // Act
        var result = accountService.getAccounts(null, AccountType.CHECKING, null, pageable);

        // Assert
        assertThat(result.content()).hasSize(1);
    }

    @Test
    @DisplayName("getAccounts_shouldFilterByCurrency - filters accounts by currency")
    void getAccounts_shouldFilterByCurrency() {
        // Arrange
        var account = sampleAccount();
        var response = sampleAccountResponse();
        var pageable = PageRequest.of(0, 10, Sort.by("name"));
        Page<Account> page = new PageImpl<>(List.of(account), pageable, 1);

        when(accountRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);
        when(accountMapper.toResponse(account)).thenReturn(response);

        // Act
        var result = accountService.getAccounts(null, null, "USD", pageable);

        // Assert
        assertThat(result.content()).hasSize(1);
    }

    @Test
    @DisplayName("updateAccount_shouldReturnResponse_whenValidRequest - updates only currency on the account")
    void updateAccount_shouldReturnResponse_whenValidRequest() {
        // Arrange
        var request = new CurrencyUpdateRequest("EUR");
        var account = sampleAccount();
        var updatedResponse = new AccountResponse(1L, "Checking123", AccountType.CHECKING,
                new BigDecimal("1000.00"), "EUR", LocalDateTime.now());

        when(accountRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(account));
        when(accountRepository.save(account)).thenReturn(account);
        when(accountMapper.toResponse(account)).thenReturn(updatedResponse);

        // Act
        var result = accountService.updateAccount(1L, request);

        // Assert
        assertThat(result).isEqualTo(updatedResponse);
        verify(accountMapper).updateEntity(request, account);
        verify(accountRepository).save(account);
    }

    @Test
    @DisplayName("updateAccount_shouldThrow_whenNotFound - throws for nonexistent id")
    void updateAccount_shouldThrow_whenNotFound() {
        // Arrange
        var request = new CurrencyUpdateRequest("EUR");

        when(accountRepository.findByIdAndUserId(999L, 1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> accountService.updateAccount(999L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Account not found");
    }

    @Test
    @DisplayName("updateAccountType_shouldReturnResponse_whenAdmin - updates account type for admin")
    void updateAccountType_shouldReturnResponse_whenAdmin() {
        // Arrange
        var request = new UpdateAccountTypeRequest(AccountType.SAVINGS);
        var account = sampleAccount();
        var savingsEntity = new AccountTypeEntity();
        savingsEntity.setId(2L);
        savingsEntity.setName("SAVINGS");
        var response = new AccountResponse(1L, "Checking123", AccountType.SAVINGS,
                new BigDecimal("1000.00"), "USD", LocalDateTime.now());

        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(accountTypeRepository.findByName("SAVINGS")).thenReturn(Optional.of(savingsEntity));
        when(accountRepository.save(account)).thenReturn(account);
        when(accountMapper.toResponse(account)).thenReturn(response);

        // Act
        var result = accountService.updateAccountType(1L, request);

        // Assert
        assertThat(result).isEqualTo(response);
        assertThat(account.getType().getName()).isEqualTo("SAVINGS");
        verify(accountRepository).save(account);
    }

    @Test
    @DisplayName("updateAccountType_shouldThrow_whenNotFound - throws ResourceNotFoundException for nonexistent account")
    void updateAccountType_shouldThrow_whenNotFound() {
        // Arrange
        var request = new UpdateAccountTypeRequest(AccountType.SAVINGS);

        when(accountRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> accountService.updateAccountType(999L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Account not found with id: 999");
    }

    @Test
    @DisplayName("createAccount_shouldThrow_whenDuplicateName - throws DuplicateResourceException for duplicate name")
    void createAccount_shouldThrow_whenDuplicateName() {
        // Arrange
        var request = new AccountCreateRequest("Checking123", AccountType.CHECKING, "USD", new BigDecimal("1000.00"));

        when(accountRepository.existsByUserIdAndName(1L, "Checking123")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> accountService.createAccount(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Account with name 'Checking123' already exists");
    }

    @Test
    @DisplayName("deleteAccount_shouldDelete_whenFound - removes account from repository")
    void deleteAccount_shouldDelete_whenFound() {
        // Arrange
        var account = sampleAccount();

        when(accountRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(account));

        // Act
        accountService.deleteAccount(1L);

        // Assert
        verify(accountRepository).delete(account);
    }

    @Test
    @DisplayName("deleteAccount_shouldThrow_whenNotFound - throws for nonexistent id")
    void deleteAccount_shouldThrow_whenNotFound() {
        // Arrange
        when(accountRepository.findByIdAndUserId(999L, 1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> accountService.deleteAccount(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Account not found");
    }
}
