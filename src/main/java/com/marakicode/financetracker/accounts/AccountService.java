package com.marakicode.financetracker.accounts;

import com.marakicode.financetracker.accounts.dto.AccountCreateRequest;
import com.marakicode.financetracker.accounts.dto.AccountResponse;
import com.marakicode.financetracker.accounts.dto.CurrencyUpdateRequest;
import com.marakicode.financetracker.accounts.dto.UpdateAccountTypeRequest;
import com.marakicode.financetracker.common.CurrentUserProvider;
import com.marakicode.financetracker.common.DuplicateResourceException;
import com.marakicode.financetracker.common.PagedResponse;
import com.marakicode.financetracker.common.ResourceNotFoundException;
import com.marakicode.financetracker.users.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final AccountTypeRepository accountTypeRepository;
    private final CurrentUserProvider currentUserProvider;
    private final UserRepository userRepository;
    private final AccountMapper accountMapper;

    @Transactional
    public AccountResponse createAccount(AccountCreateRequest request) {
        Long userId = currentUserProvider.getCurrentUserId();
        validateUniqueName(userId, request.name());
        Account account = accountMapper.toEntity(request);
        account.setUser(userRepository.getReferenceById(userId));
        account.setBalance(request.initialBalance());
        account.setType(resolveType(request.type()));
        Account saved = accountRepository.save(account);
        log.info("event=account.created accountId={} name={} userId={}", saved.getId(), request.name(), userId);
        return accountMapper.toResponse(saved);
    }

    private AccountTypeEntity resolveType(AccountType type) {
        return accountTypeRepository.findByName(type.name())
                .orElseThrow(() -> new AccountTypeNotFoundException("Unknown account type: " + type));
    }

    @Transactional(readOnly = true)
    public AccountResponse getAccountById(Long id) {
        Long userId = currentUserProvider.getCurrentUserId();
        Account account = findOwnedAccount(id, userId);
        return accountMapper.toResponse(account);
    }

    @Transactional(readOnly = true)
    public PagedResponse<AccountResponse> getAccounts(String search, AccountType type, String currency, Pageable pageable) {
        Long userId = currentUserProvider.getCurrentUserId();

        List<Specification<Account>> specs = new ArrayList<>();
        specs.add(AccountSpecification.userIdEquals(userId));

        if (search != null && !search.isBlank()) {
            specs.add(AccountSpecification.nameContains(search));
        }
        if (type != null) {
            specs.add(AccountSpecification.typeEquals(type));
        }
        if (currency != null && !currency.isBlank()) {
            specs.add(AccountSpecification.currencyEquals(currency));
        }

        Specification<Account> spec = specs.stream()
                .reduce(Specification::and)
                .orElse(null);

        var page = accountRepository.findAll(spec, pageable);
        return PagedResponse.fromPage(page, accountMapper::toResponse);
    }

    @Transactional
    public AccountResponse updateAccount(Long id, CurrencyUpdateRequest request) {
        Long userId = currentUserProvider.getCurrentUserId();
        Account account = findOwnedAccount(id, userId);
        accountMapper.updateEntity(request, account);
        Account saved = accountRepository.save(account);
        log.info("event=account.currency_updated accountId={} currency={}", id, request.currency());
        return accountMapper.toResponse(saved);
    }

    @Transactional
    public AccountResponse updateAccountType(Long id, UpdateAccountTypeRequest request) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with id: " + id));
        account.setType(resolveType(request.type()));
        Account saved = accountRepository.save(account);
        log.info("event=account.type_updated accountId={} type={}", id, request.type());
        return accountMapper.toResponse(saved);
    }

    @Transactional
    public void deleteAccount(Long id) {
        Long userId = currentUserProvider.getCurrentUserId();
        Account account = findOwnedAccount(id, userId);
        accountRepository.delete(account);
        log.info("event=account.deleted accountId={}", id);
    }

    private void validateUniqueName(Long userId, String name) {
        if (accountRepository.existsByUserIdAndName(userId, name)) {
            throw new DuplicateResourceException("Account with name '" + name + "' already exists");
        }
    }

    private Account findOwnedAccount(Long accountId, Long userId) {
        return accountRepository.findByIdAndUserId(accountId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with id: " + accountId));
    }

}
