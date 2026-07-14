package com.marakicode.financetracker.accounts;

import com.marakicode.financetracker.accounts.dto.AccountStatistics;
import com.marakicode.financetracker.accounts.dto.AccountSummary;
import com.marakicode.financetracker.common.PagedResponse;
import com.marakicode.financetracker.common.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AccountsFacadeImpl implements AccountsFacade {

    private final AccountRepository accountRepository;

    @Override
    @Transactional(readOnly = true)
    public AccountSummary getAccountById(Long accountId) {
        var account = findAccountOrThrow(accountId);
        return toSummary(account);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<AccountSummary> listAccounts(String search, Pageable pageable) {
        var spec = (search != null && !search.isBlank())
                ? AccountSpecification.nameContains(search)
                : Specification.<Account>unrestricted();
        var page = accountRepository.findAll(spec, pageable);
        return PagedResponse.fromPage(page, this::toSummary);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<AccountSummary> listUserAccounts(Long userId, Pageable pageable) {
        var spec = AccountSpecification.userIdEquals(userId);
        var page = accountRepository.findAll(spec, pageable);
        return PagedResponse.fromPage(page, this::toSummary);
    }

    @Override
    @Transactional(readOnly = true)
    public AccountStatistics getStatistics() {
        long total = accountRepository.count();
        long frozen = accountRepository.countByFrozen(true);
        var totalBalance = accountRepository.getTotalBalance();
        var byType = buildByTypeMap();
        return new AccountStatistics(total, frozen, totalBalance, byType);
    }

    @Override
    @Transactional
    public void freezeAccount(Long accountId) {
        var account = findAccountOrThrow(accountId);
        account.setFrozen(true);
        accountRepository.save(account);
    }

    @Override
    @Transactional
    public void unfreezeAccount(Long accountId) {
        var account = findAccountOrThrow(accountId);
        account.setFrozen(false);
        accountRepository.save(account);
    }

    private Account findAccountOrThrow(Long accountId) {
        return accountRepository.findById(accountId)
            .orElseThrow(() -> new ResourceNotFoundException("Account not found with id: " + accountId));
    }

    private AccountSummary toSummary(Account account) {
        var user = account.getUser();
        String userName = user.getFirstName() + " " + user.getLastName();
        return new AccountSummary(
            account.getId(), user.getId(), userName,
            account.getName(),
            AccountType.valueOf(account.getType().getName()),
            account.getBalance(), account.getCurrency(),
            account.isFrozen(), account.getCreatedAt());
    }

    private Map<String, Long> buildByTypeMap() {
        Map<String, Long> byType = new HashMap<>();
        for (Object[] row : accountRepository.countByType()) {
            byType.put((String) row[0], ((Number) row[1]).longValue());
        }
        return byType;
    }
}
