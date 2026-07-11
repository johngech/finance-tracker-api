package com.marakicode.financetracker.accounts;

import com.marakicode.financetracker.accounts.dto.AccountStatistics;
import com.marakicode.financetracker.accounts.dto.AccountSummary;
import com.marakicode.financetracker.common.ResourceNotFoundException;
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
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountsFacadeImplTest {

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private AccountsFacadeImpl accountsFacade;

    private User userEntity;
    private AccountTypeEntity accountTypeEntity;

    @BeforeEach
    void setUp() {
        userEntity = new User();
        userEntity.setId(1L);
        userEntity.setFirstName("Alice");
        userEntity.setLastName("Smith");

        accountTypeEntity = new AccountTypeEntity();
        accountTypeEntity.setId(1L);
        accountTypeEntity.setName("CHECKING");
    }

    private Account sampleAccount() {
        Account account = new Account();
        account.setId(1L);
        account.setUser(userEntity);
        account.setName("Checking123");
        account.setType(accountTypeEntity);
        account.setBalance(new BigDecimal("1000.00"));
        account.setCurrency("USD");
        account.setFrozen(false);
        ReflectionTestUtils.setField(account, "createdAt", LocalDateTime.of(2025, 1, 15, 10, 0));
        return account;
    }

    @Test
    @DisplayName("getAccountById_validId_returnsAccountSummary")
    void getAccountById_validId_returnsAccountSummary() {
        var account = sampleAccount();
        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));

        var result = accountsFacade.getAccountById(1L);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.userId()).isEqualTo(1L);
        assertThat(result.userName()).isEqualTo("Alice Smith");
        assertThat(result.name()).isEqualTo("Checking123");
        assertThat(result.type()).isEqualTo(AccountType.CHECKING);
        assertThat(result.balance()).isEqualByComparingTo(new BigDecimal("1000.00"));
        assertThat(result.currency()).isEqualTo("USD");
        assertThat(result.frozen()).isFalse();
        assertThat(result.createdAt()).isEqualTo(LocalDateTime.of(2025, 1, 15, 10, 0));
    }

    @Test
    @DisplayName("getAccountById_invalidId_throwsResourceNotFoundException")
    void getAccountById_invalidId_throwsResourceNotFoundException() {
        when(accountRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountsFacade.getAccountById(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Account not found with id: 999");
    }

    @Test
    @DisplayName("listAccounts_noSearch_returnsPagedAccounts")
    void listAccounts_noSearch_returnsPagedAccounts() {
        var account = sampleAccount();
        var pageable = PageRequest.of(0, 10, Sort.by("name"));
        Page<Account> page = new PageImpl<>(List.of(account), pageable, 1);
        when(accountRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

        var result = accountsFacade.listAccounts(null, pageable);

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).name()).isEqualTo("Checking123");
        assertThat(result.page()).isEqualTo(0);
        assertThat(result.size()).isEqualTo(10);
        assertThat(result.totalPages()).isEqualTo(1);
    }

    @Test
    @DisplayName("listAccounts_withSearch_returnsFilteredAccounts")
    void listAccounts_withSearch_returnsFilteredAccounts() {
        var account = sampleAccount();
        var pageable = PageRequest.of(0, 10, Sort.by("name"));
        Page<Account> page = new PageImpl<>(List.of(account), pageable, 1);
        when(accountRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

        var result = accountsFacade.listAccounts("Checking", pageable);

        assertThat(result.content()).hasSize(1);
        verify(accountRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    @DisplayName("listUserAccounts_validUserId_returnsUserAccounts")
    void listUserAccounts_validUserId_returnsUserAccounts() {
        var account = sampleAccount();
        var pageable = PageRequest.of(0, 10, Sort.by("name"));
        Page<Account> page = new PageImpl<>(List.of(account), pageable, 1);
        when(accountRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

        var result = accountsFacade.listUserAccounts(1L, pageable);

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).userId()).isEqualTo(1L);
        verify(accountRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    @DisplayName("getStatistics_returnsCorrectCounts")
    void getStatistics_returnsCorrectCounts() {
        when(accountRepository.count()).thenReturn(10L);
        when(accountRepository.countByFrozen(true)).thenReturn(2L);
        when(accountRepository.getTotalBalance()).thenReturn(new BigDecimal("50000.00"));
        when(accountRepository.countByType()).thenReturn(
            List.of(new Object[]{"CHECKING", 6L}, new Object[]{"SAVINGS", 4L}));

        AccountStatistics stats = accountsFacade.getStatistics();

        assertThat(stats.totalAccounts()).isEqualTo(10L);
        assertThat(stats.frozenAccounts()).isEqualTo(2L);
        assertThat(stats.totalBalance()).isEqualByComparingTo(new BigDecimal("50000.00"));
        assertThat(stats.accountsByType()).containsEntry("CHECKING", 6L).containsEntry("SAVINGS", 4L);
    }

    @Test
    @DisplayName("freezeAccount_validId_setsFrozenTrue")
    void freezeAccount_validId_setsFrozenTrue() {
        var account = sampleAccount();
        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(accountRepository.save(any(Account.class))).thenReturn(account);

        accountsFacade.freezeAccount(1L);

        assertThat(account.isFrozen()).isTrue();
        verify(accountRepository).save(account);
    }

    @Test
    @DisplayName("freezeAccount_invalidId_throwsResourceNotFoundException")
    void freezeAccount_invalidId_throwsResourceNotFoundException() {
        when(accountRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountsFacade.freezeAccount(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Account not found with id: 999");
    }

    @Test
    @DisplayName("unfreezeAccount_validId_setsFrozenFalse")
    void unfreezeAccount_validId_setsFrozenFalse() {
        var account = sampleAccount();
        account.setFrozen(true);
        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(accountRepository.save(any(Account.class))).thenReturn(account);

        accountsFacade.unfreezeAccount(1L);

        assertThat(account.isFrozen()).isFalse();
        verify(accountRepository).save(account);
    }

    @Test
    @DisplayName("unfreezeAccount_invalidId_throwsResourceNotFoundException")
    void unfreezeAccount_invalidId_throwsResourceNotFoundException() {
        when(accountRepository.findById(999L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> accountsFacade.unfreezeAccount(999L))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("listUserAccounts_emptyResult_returnsEmptyPage")
    void listUserAccounts_emptyResult_returnsEmptyPage() {
        var pageable = PageRequest.of(0, 10, Sort.by("name"));
        Page<Account> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);
        when(accountRepository.findAll(any(Specification.class), any(Pageable.class)))
            .thenReturn(emptyPage);

        var result = accountsFacade.listUserAccounts(1L, pageable);

        assertThat(result.content()).isEmpty();
    }

    @Test
    @DisplayName("getStatistics_errorPropagation_throwsException")
    void getStatistics_errorPropagation_throwsException() {
        when(accountRepository.getTotalBalance()).thenThrow(new RuntimeException("DB error"));
        assertThatThrownBy(() -> accountsFacade.getStatistics())
            .isInstanceOf(RuntimeException.class);
    }
}
