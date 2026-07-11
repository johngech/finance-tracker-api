package com.marakicode.financetracker.admin;

import com.marakicode.financetracker.accounts.AccountsFacade;
import com.marakicode.financetracker.accounts.dto.AccountStatistics;
import com.marakicode.financetracker.accounts.dto.AccountSummary;
import com.marakicode.financetracker.common.PagedResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminAccountService {

    private final AccountsFacade accountsFacade;

    public PagedResponse<AccountSummary> listAccounts(String search, Pageable pageable) {
        return accountsFacade.listAccounts(search, pageable);
    }

    public AccountSummary getAccount(Long accountId) {
        return accountsFacade.getAccountById(accountId);
    }

    public void freezeAccount(Long accountId) {
        accountsFacade.freezeAccount(accountId);
    }

    public void unfreezeAccount(Long accountId) {
        accountsFacade.unfreezeAccount(accountId);
    }

    public AccountStatistics getStatistics() {
        return accountsFacade.getStatistics();
    }
}
