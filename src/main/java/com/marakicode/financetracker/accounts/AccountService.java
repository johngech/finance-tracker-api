package com.marakicode.financetracker.accounts;

import com.marakicode.financetracker.accounts.dto.AccountCreateRequest;
import com.marakicode.financetracker.accounts.dto.AccountResponse;
import com.marakicode.financetracker.accounts.dto.CurrencyUpdateRequest;
import com.marakicode.financetracker.accounts.dto.UpdateAccountTypeRequest;
import com.marakicode.financetracker.common.DuplicateResourceException;
import com.marakicode.financetracker.common.PagedResponse;
import com.marakicode.financetracker.common.ResourceNotFoundException;
import com.marakicode.financetracker.users.Role;
import com.marakicode.financetracker.users.User;
import com.marakicode.financetracker.users.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final AccountTypeRepository accountTypeRepository;
    private final UserService userService;
    private final AccountMapper accountMapper;

    @Transactional
    public AccountResponse createAccount(AccountCreateRequest request) {
        User user = getCurrentUser();
        validateUniqueName(user.getId(), request.name());
        Account account = accountMapper.toEntity(request);
        account.setUser(user);
        account.setBalance(request.initialBalance());
        account.setType(resolveType(request.type()));
        Account saved = accountRepository.save(account);
        return accountMapper.toResponse(saved);
    }

    private AccountTypeEntity resolveType(AccountType type) {
        return accountTypeRepository.findByName(type.name())
                .orElseThrow(() -> new IllegalArgumentException("Unknown account type: " + type));
    }

    @Transactional(readOnly = true)
    public AccountResponse getAccountById(Long id) {
        User user = getCurrentUser();
        Account account = findOwnedAccount(id, user.getId());
        return accountMapper.toResponse(account);
    }

    @Transactional(readOnly = true)
    public PagedResponse<AccountResponse> getAccounts(String search, AccountType type, String currency, Pageable pageable) {
        User user = getCurrentUser();

        List<Specification<Account>> specs = new ArrayList<>();
        specs.add(AccountSpecification.userIdEquals(user.getId()));

        if (search != null && !search.isBlank()) {
            specs.add(AccountSpecification.nameContains(search));
        }
        if (type != null) {
            specs.add(AccountSpecification.typeEquals(type));
        }
        if (currency != null && !currency.isBlank()) {
            specs.add(AccountSpecification.currencyEquals(currency));
        }

        Specification<Account> spec = specs.stream().reduce(Specification::and).orElse(null);

        var page = accountRepository.findAll(spec, pageable);
        return PagedResponse.fromPage(page, accountMapper::toResponse);
    }

    @Transactional
    public AccountResponse updateAccount(Long id, CurrencyUpdateRequest request) {
        User user = getCurrentUser();
        Account account = findOwnedAccount(id, user.getId());
        accountMapper.updateEntity(request, account);
        Account saved = accountRepository.save(account);
        return accountMapper.toResponse(saved);
    }

    @Transactional
    public AccountResponse updateAccountType(Long id, UpdateAccountTypeRequest request) {
        User user = getCurrentUser();
        if (user.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Only administrators can change account types");
        }
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with id: " + id));
        account.setType(resolveType(request.type()));
        Account saved = accountRepository.save(account);
        return accountMapper.toResponse(saved);
    }

    @Transactional
    public void deleteAccount(Long id) {
        User user = getCurrentUser();
        Account account = findOwnedAccount(id, user.getId());
        accountRepository.delete(account);
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

    private User getCurrentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            throw new ResourceNotFoundException("Not authenticated");
        }
        String email = auth.getName();
        return userService.findByEmail(email);
    }
}
