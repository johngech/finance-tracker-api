package com.marakicode.financetracker.accounts;

import com.marakicode.financetracker.accounts.dto.AccountStatistics;
import com.marakicode.financetracker.accounts.dto.AccountSummary;
import com.marakicode.financetracker.common.PagedResponse;
import org.springframework.data.domain.Pageable;

public interface AccountsFacade {
    AccountSummary getAccountById(Long accountId);
    PagedResponse<AccountSummary> listAccounts(String search, Pageable pageable);
    PagedResponse<AccountSummary> listUserAccounts(Long userId, Pageable pageable);
    AccountStatistics getStatistics();
    void freezeAccount(Long accountId);
    void unfreezeAccount(Long accountId);
}
